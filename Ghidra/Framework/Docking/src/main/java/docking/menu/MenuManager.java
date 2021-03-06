/* ###
 * IP: GHIDRA
 * REVIEWED: YES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package docking.menu;

import java.util.*;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;

import docking.action.DockingActionIf;
import docking.action.MenuData;

/**
 * Class to manage a hierarchy of menus.
 */
public class MenuManager implements ManagedMenuItem {
	private static String NULL_GROUP_NAME = "<null group>";

	private Set<ManagedMenuItem> managedMenuItems = new HashSet<ManagedMenuItem>();
	private Map<String, MenuManager> subMenus = new HashMap<String, MenuManager>();

	private String name;
	private final String[] menuPath;
	private char mnemonicKey = '\0';
	private int level;
	private boolean usePopupPath;
	private MenuHandler menuHandler;
	private String group;
	private JPopupMenu popupMenu;
	private JMenu menu;
	private MenuGroupMap menuGroupMap;
	private Comparator<ManagedMenuItem> comparator;

	/**
	 * Constructs a new MenuManager
	 * @param name the name of the menu.
	 * @param mnemonicKey the key to use for the menu mnemonic
	 * @param group the group of the menu.
	 * @param showKeyBindings if true, includes the keybinding text on the menu item.
	 * @param usePopupPath if true, registers actions with popup paths as popup items.
	 * @param menuHandler Listener to be notified of menu behavior.
	 */
	public MenuManager(String name, char mnemonicKey, String group, boolean usePopupPath,
			MenuHandler menuHandler, MenuGroupMap menuGroupMap) {
		this(name, new String[] { name }, mnemonicKey, 0, group, usePopupPath, menuHandler,
			menuGroupMap);
	}

	/**
	 * Constructs a new MenuManager at the given level. The level corresponds to how deep this menu
	 * is within other menus.
	 * @param name the name of this menu.
	 * @param menuPath the path of the menu item represented by this menu manager.
	 * @param mnemonicKey the key to use for the menu mnemonic
	 * @param level the number of parent menus that this menu is in.
	 * @param group the group of this menu.
	 * @param showKeyBindings if true, includes the keybinding text on the menu item.
	 * @param usePopupPath if true, registers actions with popup paths as popup items.
	 * @param menuHandler Listener to be notified of menu behavior.
	 */
	MenuManager(String name, String[] menuPath, char mnemonicKey, int level, String group,
			boolean usePopupPath, MenuHandler menuHandler, MenuGroupMap menuGroupMap) {
		this.name = name;
		this.menuPath = menuPath;
		this.mnemonicKey = mnemonicKey;
		this.level = level;
		this.menuGroupMap = menuGroupMap;
		if (menuGroupMap == null) {
			this.menuGroupMap = new MenuGroupMap();
		}

		this.group = group;
		this.usePopupPath = usePopupPath;
		this.menuHandler = menuHandler;

		if (usePopupPath) {
			comparator = new ManagedMenuItemComparator(new PopupGroupComparator());
		}
		else {
			comparator = new ManagedMenuItemComparator(new GroupComparator());
		}
	}

	/**
	 * Adds an action to this menu. Can create subMenus depending on the menuPath of the
	 * action.
	 * @param action the action to be added.
	 * @param menuGroupMap group map for menuItems
	 */
	public void addAction(DockingActionIf action) {
		checkForSwingThread();
		resetMenus();
		MenuData menuData = usePopupPath ? action.getPopupMenuData() : action.getMenuBarData();
		String[] actionMenuPath = menuData.getMenuPath();
		if (actionMenuPath.length > level + 1) {
			String subMenuName = actionMenuPath[level];
			String cleanSubMenuName = stripMnemonicAmp(subMenuName);
			MenuManager mgr = subMenus.get(cleanSubMenuName);

			if (mgr == null) {
				char mnemonic = getMnemonicKey(subMenuName);

				int submenuLevel = level + 1;
				String[] submenuPath = new String[submenuLevel];
				System.arraycopy(actionMenuPath, 0, submenuPath, 0, submenuLevel);
				String submenuGroup = menuGroupMap.getMenuGroup(submenuPath);
				if (submenuGroup == null) {
					submenuGroup = subMenuName;
				}

				mgr =
					new MenuManager(cleanSubMenuName, submenuPath, mnemonic, submenuLevel,
						submenuGroup, usePopupPath, menuHandler, menuGroupMap);
				subMenus.put(cleanSubMenuName, mgr);
				managedMenuItems.add(mgr);
			}
			mgr.addAction(action);
		}
		else {
			managedMenuItems.add(new MenuItemManager(menuHandler, action, usePopupPath));
		}
	}

	public DockingActionIf getAction(String actionName) {
		for (ManagedMenuItem item : managedMenuItems) {
			if (item instanceof MenuItemManager) {
				DockingActionIf action = ((MenuItemManager) item).getAction();
				if (actionName.equals(action.getName())) {
					return action;
				}
			}
		}
		return null;
	}

	/**
	 * Parses the mnemonic key from the menu items text.
	 * @param str the menu item text
	 * @return the mnemonic key for encoded in the actions menu text. Returns 0 if there is none.
	 */
	public static char getMnemonicKey(String str) {
		int ampLoc = str.indexOf('&');
		char mk = '\0';
		if (ampLoc >= 0 && ampLoc < str.length() - 1) {
			mk = str.charAt(ampLoc + 1);
		}
		return mk;
	}

	/***
	 * Removes the Mnemonic indicator character (&) from the text.
	 * @param str the text to strip.
	 */
	public static String stripMnemonicAmp(String str) {
		int ampLoc = str.indexOf('&');
		if (ampLoc < 0) {
			return str;
		}
		String s = str.substring(0, ampLoc);
		if (ampLoc < (str.length() - 1)) {
			s += str.substring(++ampLoc);
		}
		return s;
	}

	/**
	 * Tests if this menu is empty.
	 */
	@Override
	public boolean isEmpty() {
		return managedMenuItems.isEmpty();
	}

	/**
	 * Returns a Menu hierarchy of all the actions.
	 */
	public JMenu getMenu() {
		if (menu == null) {
			menu = new JMenu(name);
			if (mnemonicKey != '\0') {
				menu.setMnemonic(mnemonicKey);
			}
			if (menuHandler != null) {
				menu.addMenuListener(menuHandler);
			}

			List<ManagedMenuItem> list = new ArrayList<ManagedMenuItem>(managedMenuItems);
			Collections.sort(list, comparator);
			String lastGroup = null;

			for (ManagedMenuItem item : list) {
				if (lastGroup != null && !lastGroup.equals(item.getGroup())) {
					menu.addSeparator();
				}
				lastGroup = item.getGroup();
				menu.add(item.getMenuItem());
			}
		}
		return menu;
	}

	/**
	 * @see docking.menu.ManagedMenuItem#getMenuItem()
	 */
	@Override
	public JMenuItem getMenuItem() {
		JMenu localMenu = getMenu();
		localMenu.setUI((DockingMenuUI) DockingMenuUI.createUI(localMenu));
		return localMenu;
	}

	/**
	 * @see docking.menu.ManagedMenuItem#getGroup()
	 */
	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public String getSubGroup() {
		String menuSubGroup = menuGroupMap.getMenuSubGroup(menuPath);
		if (menuSubGroup == null) {
			return MenuData.NO_SUBGROUP;
		}
		return menuSubGroup;
	}

	/**
	 * @see docking.menu.ManagedMenuItem#dispose()
	 */
	@Override
	public void dispose() {
		for (ManagedMenuItem item : managedMenuItems) {
			item.dispose();
		}

		subMenus.clear();
	}

	/**
	 * Returns a JPopupMenu for the action hierarchy.
	 */
	public JPopupMenu getPopupMenu() {
		if (popupMenu == null) {
			popupMenu = new JPopupMenu(name);

			List<ManagedMenuItem> list = new ArrayList<ManagedMenuItem>(managedMenuItems);
			Collections.sort(list, comparator);
			String lastGroup = NULL_GROUP_NAME;
			boolean hasMenuItems = false;

			for (ManagedMenuItem item : list) {
				String itemGroup = item.getGroup();
				if (itemGroup == null) {
					itemGroup = NULL_GROUP_NAME;
				}

				if (!lastGroup.equals(itemGroup) && hasMenuItems) {
					popupMenu.addSeparator();
				}

				lastGroup = item.getGroup();
				if (lastGroup == null) {
					lastGroup = NULL_GROUP_NAME;
				}

				popupMenu.add(item.getMenuItem());
				hasMenuItems = true;
			}
		}
		return popupMenu;
	}

	private void remove(ManagedMenuItem item) {
		managedMenuItems.remove(item);
		if (item instanceof MenuManager) {
			subMenus.remove(((MenuManager) item).name);
		}
		item.dispose();
	}

	@Override
	public boolean removeAction(DockingActionIf action) {
		for (ManagedMenuItem item : managedMenuItems) {
			if (item.removeAction(action)) {
				if (item.isEmpty()) {
					remove(item);
				}
				resetMenus();
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return name;
	}

	private void resetMenus() {
		popupMenu = null;
		menu = null;
	}

	/**
	 * Notification that a menu item has changed groups.
	 * @param theMenuPath the menu path of the item whose group changed.
	 * @param i the index into the menu path of the part that changed groups.
	 * @param localGroup the new group.
	 */
	public void menuGroupChanged(String[] theMenuPath, int i, String localGroup) {
		checkForSwingThread();
		resetMenus();
	}

	private void checkForSwingThread() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new RuntimeException("Calls to MenuManager must be in the Swing Thread!");
		}
	}

	public PopupMenuListener getMenuHandler() {
		return menuHandler;
	}

	@Override
	public String getMenuItemText() {
		return name;
	}

//==================================================================================================
// Inner Classes
//==================================================================================================

	/** 
	 * This comparator puts null grouped items at the bottom of menus for menu bar menus so that
	 * the ungrouped items will cluster at the end.
	 */
	private class GroupComparator implements Comparator<String> {

		@Override
		public int compare(String group1, String group2) {
			if (group1 == null && group2 == null) {
				return 0;
			}
			if (group1 == null) {
				return 1;
			}
			if (group2 == null) {
				return -1;
			}
			return group1.compareTo(group2);
		}
	}

	/** 
	 * This comparator puts null grouped items at the top of the menu so that universal popup
	 * actions are always at the bottom (e.g., Copy for tables).
	 */
	private class PopupGroupComparator implements Comparator<String> {

		@Override
		public int compare(String group1, String group2) {
			if (group1 == null && group2 == null) {
				return 0;
			}
			if (group1 == null) {
				return -1;
			}
			if (group2 == null) {
				return 1;
			}
			return group1.compareTo(group2);
		}
	}

	private class ManagedMenuItemComparator implements Comparator<ManagedMenuItem> {

		private final Comparator<String> groupComparator;

		ManagedMenuItemComparator(Comparator<String> groupComparator) {
			this.groupComparator = groupComparator;
		}

		@Override
		public int compare(ManagedMenuItem m1, ManagedMenuItem m2) {
			int result = groupComparator.compare(m1.getGroup(), m2.getGroup());
			if (result != 0) {
				return result;
			}

			// the groups are the same, check the subgroups
			String subGroup1 = m1.getSubGroup();
			String subGroup2 = m2.getSubGroup();

			result = subGroup1.compareTo(subGroup2);
			if (result != 0) {
				return result;
			}

			// when the group is the same, sub-sort by the item's name
			String text1 = m1.getMenuItemText();
			String text2 = m2.getMenuItemText();

			result = text1.compareTo(text2);
			if (result == 0) {
				// When the names are the same, we have to compare even further, or the items
				// will not all appear in the set, as the set will consider them equal. Just use
				// something unique that is guaranteed not to change in this VM session.  This 
				// will only be equal if the objects are the same reference.
				return System.identityHashCode(m1) - System.identityHashCode(m2);
			}
			return result;
		}

	}
}
