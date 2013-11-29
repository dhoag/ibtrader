package com.davehoag.ib.tools;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelToCSV{
    static void xlsx(File inputFile, File outputFile) {

        try {
        	FileOutputStream f = new FileOutputStream(outputFile);
            BufferedOutputStream bos = new BufferedOutputStream(f);
            
            // Get the workbook object for XLSX file
            
            Workbook wBook = WorkbookFactory.create(new BufferedInputStream(new FileInputStream(inputFile)));
            // Get first sheet from the workbook
            Sheet sheet = wBook.getSheetAt(0);
            writeRows(bos, sheet);
            bos.close();
            f.close();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }
    //testing the application 

	/**
	 * @param bos
	 * @param sheet
	 * @throws IOException
	 */
	protected static void writeRows(BufferedOutputStream bos, Sheet sheet) throws IOException {
		Row row;
		// Iterate through each rows from first sheet
		Iterator<Row> rowIterator = sheet.iterator();

		StringBuffer data = new StringBuffer();

		while (rowIterator.hasNext()) {
		    row = rowIterator.next();
		    writeColumns(data, row);
		    data.append("\r\n"); 
		}

		bos.write(data.toString().getBytes());
	}

	/**
	 * @param data
	 * @param row
	 */
	protected static void writeColumns(StringBuffer data, Row row) {
		Cell cell;
		// For each row, iterate through each columns
		Iterator<Cell> cellIterator = row.cellIterator();
		while (cellIterator.hasNext()) {

		    cell = cellIterator.next();

		    switch (cell.getCellType()) {
		        case Cell.CELL_TYPE_BOOLEAN:
		            data.append(cell.getBooleanCellValue() + ",");

		            break;
		        case Cell.CELL_TYPE_NUMERIC:
		            data.append(cell.getNumericCellValue() + ",");

		            break;
		        case Cell.CELL_TYPE_STRING:
		        	data.append("\"");
		            data.append(cell.getStringCellValue());
		            data.append("\",");
		            break;

		        case Cell.CELL_TYPE_BLANK:
		            data.append("" + ",");
		            break;
		        default:
		            data.append(cell + ",");

		    }
		}
	}

    public static void main(String[] args) {
        //reading file from desktop
        File inputFile = new File(args[0]);
        //writing excel data to csv 
        File outputFile = new File(args[1]);
        xlsx(inputFile, outputFile);
    }
}
