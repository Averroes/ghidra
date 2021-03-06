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
package ghidra.app.merge.datatypes;

import ghidra.app.merge.MergeConstants;
import ghidra.app.plugin.core.datamgr.archive.SourceArchive;
import ghidra.program.model.data.*;
import ghidra.program.model.data.Enum;
import ghidra.program.model.listing.FunctionSignature;
import ghidra.util.UniversalID;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.*;

/**
 * Panel to show the contents of a Data Type.
 * 
 * 
 */
class DataTypePanel extends JPanel {

	private static final long serialVersionUID = 1L;
    public Color SOURCE_COLOR = new Color(0, 140, 0);
	private DataType dataType;
	private JTextPane textPane;
	private StyledDocument doc;
	private SimpleAttributeSet pathAttrSet;
	private SimpleAttributeSet nameAttrSet;
	private SimpleAttributeSet sourceAttrSet;
	private SimpleAttributeSet contentAttrSet;
	private SimpleAttributeSet fieldNameAttrSet;
	private SimpleAttributeSet commentAttrSet;
	private SimpleAttributeSet deletedAttrSet;
	
	DataTypePanel(DataType dataType) {
		super(new BorderLayout());
		this.dataType = dataType;
		create();
	}
	void setDataType(DataType dataType) {
		this.dataType = dataType;
		textPane.setText("");

		if (dataType instanceof Composite) {
			formatCompositeText((Composite)dataType);
		}
		else if (dataType instanceof Enum) {
			formatEnumText((Enum)dataType);
		}
		else if (dataType instanceof TypeDef) {
			formatTypeDefText((TypeDef)dataType); 
		}
		else if (dataType instanceof FunctionDefinition) {
			formatFunctionDef((FunctionDefinition)dataType);
		}
		else {
			formatDataType(dataType); 
		}
		textPane.setCaretPosition(0);
	}
	private void create() {
		textPane = new JTextPane(); 
		doc = textPane.getStyledDocument();
		add(textPane, BorderLayout.CENTER);
		textPane.setEditable(false);
		
		pathAttrSet = new SimpleAttributeSet();
		pathAttrSet.addAttribute(StyleConstants.FontFamily, "Tahoma");
		pathAttrSet.addAttribute(StyleConstants.FontSize, new Integer(11));
		pathAttrSet.addAttribute(StyleConstants.Bold, Boolean.TRUE);
		pathAttrSet.addAttribute(StyleConstants.Foreground, MergeConstants.CONFLICT_COLOR);
	
		nameAttrSet = new SimpleAttributeSet();
		nameAttrSet.addAttribute(StyleConstants.FontFamily, "Tahoma");
		nameAttrSet.addAttribute(StyleConstants.FontSize, new Integer(11));
		nameAttrSet.addAttribute(StyleConstants.Bold, Boolean.TRUE);

		sourceAttrSet = new SimpleAttributeSet();
		sourceAttrSet.addAttribute(StyleConstants.FontFamily, "Tahoma");
		sourceAttrSet.addAttribute(StyleConstants.FontSize, new Integer(11));
		sourceAttrSet.addAttribute(StyleConstants.Bold, Boolean.TRUE);
		sourceAttrSet.addAttribute(StyleConstants.Foreground, SOURCE_COLOR);

		contentAttrSet = new SimpleAttributeSet();
		contentAttrSet.addAttribute(StyleConstants.FontFamily, "Monospaced");
		contentAttrSet.addAttribute(StyleConstants.FontSize, new Integer(12));
		contentAttrSet.addAttribute(StyleConstants.Foreground, Color.BLUE);
	
		fieldNameAttrSet = new SimpleAttributeSet();
		fieldNameAttrSet.addAttribute(StyleConstants.FontFamily, "Monospaced");
		fieldNameAttrSet.addAttribute(StyleConstants.FontSize, new Integer(12));
		fieldNameAttrSet.addAttribute(StyleConstants.Foreground, new Color(204, 0, 204));

		commentAttrSet = new SimpleAttributeSet();
		commentAttrSet.addAttribute(StyleConstants.FontFamily, "Monospaced");
		commentAttrSet.addAttribute(StyleConstants.FontSize, new Integer(12));
		commentAttrSet.addAttribute(StyleConstants.Foreground, new Color(0, 204, 51));

		deletedAttrSet = new SimpleAttributeSet();
		deletedAttrSet.addAttribute(StyleConstants.FontFamily, "Tahoma");
		deletedAttrSet.addAttribute(StyleConstants.FontSize, new Integer(12));
		deletedAttrSet.addAttribute(StyleConstants.Bold, Boolean.TRUE);
		deletedAttrSet.addAttribute(StyleConstants.Foreground, Color.RED);
		
		setDataType(dataType);
	}
	
	private void formatPath(DataType dt) {
		insertString("Path: " + dt.getCategoryPath()+ "\n\n", pathAttrSet);
	}
	
	private void formatSourceArchive(DataType dt) {
		insertString("Source Archive: " + getSourceArchiveName(dt) + "\n", sourceAttrSet);
	}
	
	private String getSourceArchiveName(DataType dt) {
		SourceArchive sourceArchive = dt.getSourceArchive();
		UniversalID sourceID = (sourceArchive != null) ? sourceArchive.getSourceArchiveID() : null;
		if (sourceID == null) {
			return "Local";
		}
		return sourceArchive.getName();
	}
	
	private void formatAlignment(Composite composite) {
		StringBuffer alignmentBuffer = new StringBuffer();
    	if (!composite.isInternallyAligned()) {
    		alignmentBuffer.append( "Unaligned" );
    	}
    	else if (composite.isDefaultAligned()) {
    		alignmentBuffer.append( "Aligned" );
    	}
    	else if (composite.isMachineAligned()) {
    		alignmentBuffer.append( "Machine aligned");
    	}
    	else {
        	long alignment = composite.getMinimumAlignment();
        	alignmentBuffer.append( "align(" + alignment + ")" );
    	}
    	if (composite.isInternallyAligned()) {
        	long packingValue = composite.getPackingValue();
        	if (packingValue != Composite.NOT_PACKING) {
        		alignmentBuffer.append( " pack(" + packingValue + ")" );
        	}
    	}

		insertString(alignmentBuffer.toString() + "\n\n", sourceAttrSet);
	}
	
//	private void formatAlignmentValue(Composite composite) {
//		StringBuffer alignmentBuffer = new StringBuffer();
//    	alignmentBuffer.append( "Alignment: " );
//    	
//    	DataTypeManager dataTypeManager = composite.getDataTypeManager();
//    	DataOrganization dataOrganization = null;
//    	if (dataTypeManager != null) {
//    		dataOrganization = dataTypeManager.getDataOrganization();
//    	}
//    	if (dataOrganization == null) {
//    		dataOrganization = DataOrganization.getDefaultOrganization();
//    	}
//    	int alignment = dataOrganization.getAlignment(composite, composite.getLength());
//    	alignmentBuffer.append( "" + alignment );
//
//		insertString("\n" + alignmentBuffer.toString() + "\n", sourceAttrSet);
//	}
	
	private void formatCompositeText(Composite comp) {
		formatSourceArchive(comp);
		formatPath(comp);
		formatAlignment(comp);
		insertString(comp.getDisplayName(), nameAttrSet);
		insertString(" { \n", contentAttrSet);
    	
		DataTypeComponent[] components = comp.getComponents();
		int maxLength=0;
		int maxFieldNameLength=0;
		for (int i=0; i<components.length; i++) {
			String name = components[i].getDataType().getDisplayName();
			if (name.length() > maxLength) {
				maxLength = name.length();
			}
			String fieldName = components[i].getFieldName();
			if (fieldName == null) {
				fieldName = " ";
			}
			if (fieldName.length() > maxFieldNameLength) {
				maxFieldNameLength = fieldName.length();
			}
		} 	

    	for (int i=0; i<components.length; i++) {
    		String fieldName = components[i].getFieldName();
    		if (fieldName == null) {
    			fieldName = "";
    		}
    		String comment = components[i].getComment();
    		if (comment == null) {
    			comment = "";
    		}
    		fieldName = pad(fieldName, maxFieldNameLength);
    		String typeName = pad(components[i].getDataType().getDisplayName(), maxLength);
    		
    		insertString("    " + typeName + "  ", contentAttrSet);
    		insertString(fieldName + "   ", fieldNameAttrSet);
    		insertString(comment, commentAttrSet);
    		if (i < components.length-1) {
    			insertString("\n", contentAttrSet);
    		}
     	}
    	insertString("\n }\n", contentAttrSet);
//		formatAlignmentValue(comp);
    }

	private void formatEnumText(Enum enuum) {
		formatSourceArchive(enuum);
		formatPath(enuum);
		insertString(enuum.getDisplayName(), nameAttrSet);
		insertString(" { \n", contentAttrSet);

		StringBuffer sb = new StringBuffer();
   	
   		String[] names = enuum.getNames();  
		int maxLength = 0;
		for (int i=0; i<names.length; i++) {
			if (names[i].length() > maxLength) {
				maxLength = names[i].length();
			}
		}   		
		long[] values = enuum.getValues();
		Arrays.sort(values);
		
   		for (int i=0; i<values.length; i++) {
			String name = enuum.getName(values[i]);
   			name = pad(name, maxLength);
   			sb.append("    " + name + " = 0x" + Long.toHexString(values[i]) + " ");
   			if (i < values.length-1) {
   				sb.append("\n");
   			}
   		}
		sb.append("\n }\n");
		insertString(sb.toString(), contentAttrSet);
    }
	private void formatTypeDefText(TypeDef td) {
		formatSourceArchive(td);
		formatPath(td);
		insertString(td.getDisplayName(), nameAttrSet);
		insertString("\n", contentAttrSet);
		insertString("     TypeDef on " + td.getDataType().getDisplayName(),
				contentAttrSet);
	}
	
	private void formatFunctionDef(FunctionDefinition fd) {
		formatSourceArchive(fd);
		formatPath(fd);
		ParameterDefinition[] vars = fd.getArguments();
		
		DataType returnType = fd.getReturnType();
		insertString(returnType.getDisplayName(), contentAttrSet);
		insertString("  " + fd.getDisplayName(), nameAttrSet);
		insertString(" (", contentAttrSet);
        boolean hasVarArgs = fd.hasVarArgs();
		if ((vars.length == 0) && !hasVarArgs) {
			insertString(")", contentAttrSet);
			return;
		}
		int maxLength = 0;
		for (int i=0; i<vars.length; i++) {
			String typeName = vars[i].getDataType().getDisplayName();
			if (typeName.length() > maxLength) {
				maxLength = typeName.length();
			}
		}   		

		StringBuffer sb = new StringBuffer();
		for (int i=0; i<vars.length; i++) {
			sb.append("\n");
			String name = vars[i].getDataType().getDisplayName();
  			name = pad(name, maxLength);
			
			sb.append("    " + name + " " + vars[i].getName());
            if ((i < vars.length-1) || (vars.length > 0 && hasVarArgs)) {
				sb.append(",");
			}
		}
        if (hasVarArgs) {
        	if (vars.length > 0) {
                sb.append( "\n" ).append( "    " );
        	}
            sb.append( FunctionSignature.VAR_ARGS_DISPLAY_STRING );
        }
		sb.append(")");
		insertString(sb.toString(), contentAttrSet);
	}

	private void formatDataType(DataType dt) {
		if (dt == null) {
			insertString("\n\nDeleted", deletedAttrSet); 
			return;
		}
		formatSourceArchive(dt);
		formatPath(dt);
		insertString(dt.getDisplayName(), nameAttrSet);
	}
	
	private String pad(String str, int length) {
		StringBuffer sb = new StringBuffer(str);
		int len = length - str.length();
		for (int i=0; i<len; i++) {
			sb.append(" ");
		}
		return sb.toString();
	}
	private void insertString(String str, SimpleAttributeSet attrSet) {
		int offset = doc.getLength();

		try {
			doc.insertString(offset, str, attrSet);
		} catch (BadLocationException e1) {
		}
	}	
}
