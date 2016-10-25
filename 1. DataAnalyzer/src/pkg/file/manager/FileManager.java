package pkg.file.manager;

import Consts.Consts;

import java.io.*; // File
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

public class FileManager {	

	public List<String> GetFileLines(String filePath) {		
		List<String> lines = new ArrayList();
		File file = new File(filePath);
		BufferedReader reader= null;
				
		try {
			FileReader fileReader = new FileReader(file);			
			reader = new BufferedReader(fileReader);

			String text = null;
			while ((text = reader.readLine()) != null) {
				lines.add(text);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}

		return lines;

	}
	
	public static void main(String args[]){
		FileManager ana = new FileManager();
		List<String> lines = ana.GetFileLines("df");
		
		for (String line : lines) {
			System.out.println(line);
		}
	}
}
