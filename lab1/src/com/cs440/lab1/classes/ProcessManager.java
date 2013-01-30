//package com.cs440.lab1.classes;
import java.util.*;
//import java.io.FileInputStream;
import java.io.IOException;
//import java.io.InputStream;
//import java.io.Serializable;

class ProcessTable {
	/*holds a slave's process, and that process's time on this processor*/
	private int time = 0;
	String hostname;
}

class SlaveHost {
	/*contains a slave to run on, and a process*/
	private String hostname;
	/*probably a better way to do this...
	  hash currently used process id's so
	  we can efficiently create new processes
	*/
	private List<Integer> process_id;
	public String getHostName() {
		return hostname;
	}
}

public class ProcessManager {
	List<SlaveHost> slave_list;
	boolean master;

	ProcessManager() {	

	}

        //void process_input(byte[]
	public static void main(String[] argv) throws IOException {
		int b;
		byte[] input;
		long length  = 0;
		long offset = 0;
		if (argv.length == 2) {
			if (argv[0] != "-c") {
				System.err.println("ERROR: Bad input");
			}
			//connect to the master
		}
		else if (argv.length != 0) {
			System.err.println("ERROR: Incorrect # of arguments");
		}


		while(true) {
			b = System.in.read();
			if (b != -1) {
				length = offset;
				//process_command(input, length);
				offset = 0;
			}
			/*load balance here*/
		}
	}
}
