//package my440package;


//TODO send slaves messages for load balancing

import java.util.*;
import java.nio.channels.*;
import java.io.BufferedReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.io.FileInputStream;
import java.io.IOException;
//import java.io.InputStream;
//import java.io.Serializable;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.*;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
/*
class MigratableProcess {
	void suspend() {}
	public String toString() {return null;}
}	
*/

/** SlaveHost contains methods for load balancing, most importantly
 *  a list of processes that its host currently holds
 *  the process list is FIFO, so processes that have been on this machine
 *  the longest will be packaged and mailed somewhere
 *  @param _iaddr  The Inet Address which the master will use to communicate
 *	          with the slave
*/
class SlaveHost {
	private InetAddress iaddr;
	private int process_count;
	private List<Integer> process_list;

	SlaveHost(InetAddress _iaddr) {
		iaddr         = _iaddr;
		process_count = 0;
	}

	public InetAddress getInetAddress() {
		return iaddr;
	}
	public Integer popProcess() {
		try {
			if (process_list.size() == 0) {
				System.err.println("popProcess: No Processes Remain.  Can't pop");
				return new Integer(-1);
			}
		} catch (Exception e) {
			return new Integer(-1);
		}
		return process_list.remove(0);
	}
	public void pushProcess(int _pid) {
		try {
			process_list.add(_pid);
		} catch (Exception e) {
			System.err.println("pushProcess " + _pid + " failed");
		}
	}
}

class SlaveMessage {
	private int processId;
	private char action;

	SlaveMessage(int pid, char act) {
		processId = pid;
		action = act;
	}

	public int getProcessId() {
		return processId;
	}

	public char getAction() {
		return action;
	}
}


public class ProcessManager {
	//The port for all the servers to run on
	private static final int MASTER_PORT   = 15440;
	private static final int SLAVE_PORT    = 15440;
	private static final String OBJECT_DIR = "/afs/ blah blah kbravo / 440";
	private static final int SOCK_TIMEOUT  = 100;
	private int currentProcessId           = 0;
	private String MASTER_HOSTNAME;

	private ServerSocket slaveServerSocket;
	private ServerSocket master_sock;

	//list of slaves that are communicating with the master
	private List<SlaveHost> slave_list;

	//mapping from processID to slave it is on.
	//TODO change the name of processMap...bad style
	private Map<Integer, SlaveHost> processMap;
	private Map<InetAddress, SlaveHost> iaddrMap;
	private Map<Integer, MigratableProcess> pidMap;

	//list of processes that need to be redistributed
	private List<Integer> suspendedProcesses;

	private boolean master;

	//private ProcessServer server;

	public ProcessManager(boolean _isMaster, String masterUrl) {	
		this.master     = _isMaster;
		MASTER_HOSTNAME = masterUrl;
		//server = new ProcessServer(port, this);
	}
	

	/**
	 * newProcessId()
	 * finds a new ProcessId and returns it
	 * if none are left, return -1
	 */

	private int newProcessId() {
		Integer res = 0;
		while (res <= Integer.MAX_VALUE) {
			if (!(processMap.containsKey(res))) {
				return res.intValue();
			}
		}
		return -1;
	}
	/**
	 * printProcesses()
	 * 
	 * Print to std out the running processes and their arguments
	 */
	public void printProcesses() {
		return;
	}
	
	/**
	 * sendProcessToSlave: opens a connection to the slave with id slaveId
	 * and sends it a message to start running process processId
	 * 
	 * @param slaveId The id of the slave to send to
	 * @param processId The id of the process to send
	 */
	private void sendProcessToSlave(int slaveId, int processId) {
			
		return;
	}

	
	/**
	 * Reads and deserializes the process with ID _id. 
	 * 
	 * @param _id The process ID to read in from disk
	 * @return The deserialized MigratableProccess with id _id
	 */
	private MigratableProcess readProcess(int _id) {
		String fileName = "processes/process_" + _id;
		MigratableProcess p;
		TransactionalFileInputStream fileStream = new TransactionalFileInputStream(fileName);

		try {
			ObjectInputStream objectStream = new ObjectInputStream(fileStream);
			p = (MigratableProcess) objectStream.readObject();
			objectStream.close();
			fileStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				fileStream.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return null;
		}
		return p;
	}
	
	/**
	 * Suspends, serializes and writes to disk the given process
	 * 
	 * @param p The migratableProcess to store
	 * @param _id The ID of this migratable process
	 */
	private void writeProcess(MigratableProcess p, int _id) {
		//suspend the process so it can be serialized
		p.suspend();
		
		TransactionalFileOutputStream fileStream = new TransactionalFileOutputStream("processes/process_"+_id+".ser");
		ObjectOutputStream objectStream;
		
		try {
			objectStream = new ObjectOutputStream(fileStream);
			objectStream.writeObject(p);
			objectStream.close();
			fileStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * addNewSlave()
	 * @param slave_host - the SlaveHost instance to add
	 * @param iaddr      - the InetAddress to map slave_host to
	 * adds a new slave to the master's pool of slaves
	 */
	private void addNewSlave(SlaveHost slave_host, InetAddress iaddr) {
		slave_list.add(slave_host);
		iaddrMap.put(iaddr, slave_host);
	}


	private void master_do(InetAddress iaddr, SlaveMessage msg) {
		int processId = msg.getProcessId();
		char action   = msg.getAction();
		if (processId != -1) {
			if (action == 'S') {
				//remove it from the processMap (it's not on
				//a slave anymore) and add to suspendedProcs
				//also remove it from the slavehost instance
				if (iaddr != null) {
					suspendedProcesses.add(new Integer(processId));
					processMap.remove(new Integer(processId));
				}
			}
			else if (action == 'T') {
				//remove from the processMap and pidMap, pop it from
				//the SlaveHost
				if (iaddr != null) {
					processMap.remove(new Integer(processId));
					pidMap.remove(new Integer(processId));
				}
			}
			else if (action == 'R') {
				//remove from suspended processes, add to processmap,
				//add to corresponding SlaveSost instance
				if (iaddr != null) {
					suspendedProcesses.remove(new Integer(processId));
					processMap.put(new Integer(processId), iaddrMap.get(iaddr));
					iaddrMap.get(iaddr).pushProcess(processId);
				}
				else {
					System.err.println("Failed to add process to running");
				}
			}
		}
	}
				

	
	/**
	 * readMessageFromSlave()
	 * @param slave_sock - the Socket from which to read the message
	 * Takes a socket connected with a slave host and reads the secret message
	 */
	private SlaveMessage readMessageFromSlave(Socket slave_sock) {
		InputStream slave_stream;
		ObjectInputStream slave_ostream;
		try {
			slave_stream  = slave_sock.getInputStream();
			slave_ostream = new ObjectInputStream(slave_stream);
		} catch (IOException e) {
			System.err.println("readMessageFromSlave: setup error");
			return null;
		}
		SlaveMessage msg;
		try {
			msg = (SlaveMessage)slave_ostream.readObject();
		} catch (Exception e) {
			//TODO maybe send a message back to the slave?
			System.err.println("readMessageFromSlave: readObject error");
			msg = null;
		}

		try {
			slave_stream.close();
			slave_ostream.close();
		} catch (Exception e) {
			System.err.println("readMessageFromSlave: close error");
		}
		return msg;
	}

	private SlaveMessage readMessageFromMaster(Socket master_sock) {
		return readMessageFromSlave(master_sock);
	}


	/**
	 * sendMessageToMaster()
	 * @param msg - the message to send
	 */
	private void sendMessageToMaster(SlaveMessage msg) {
		Socket sock;
		OutputStream os;
		ObjectOutputStream msg_os;

		try {
			sock   = new Socket(MASTER_HOSTNAME, MASTER_PORT);
			os     = sock.getOutputStream();
			msg_os = new ObjectOutputStream(os);
		} catch (Exception e) {
			System.err.println("sendMessageToMaster: setup error");
			return;
		}
		
		try {
			//write the msg to the master
			msg_os.writeObject(msg);
			os.close();
			msg_os.close();
			sock.close();
		} catch (Exception e) {
			System.err.println("Error in sendMessageToMaster()");
		}
	}

	/**
	 * sendMessageToSlave()
	 * @param msg  - the message to send
	 * @param sock - the Socket over which to send it
	 */
	private void sendMessageToSlave(SlaveMessage msg, Socket sock) {
		OutputStream os;
		ObjectOutputStream msg_os;
		try {
			os = sock.getOutputStream();
			msg_os = new ObjectOutputStream(os);
		} catch (Exception e) {
			System.err.println("sendMessageToSlave: setup error");
			return;
		}

		try {
			msg_os.writeObject(msg);
			os.close();
			msg_os.close();
		} catch (Exception e) {
			System.err.println("Error in sendMessageToSlave() write/close");
		}
	}
		
		
		


	/**********************************************
	 * slave_do()				      *
	 * @param master_msg holds the message        *
	 * takes a message from the master and does   *
	 * what it says				      *
	 **********************************************
	 */

	private int slave_do(SlaveMessage master_msg) {
		int processId = master_msg.getProcessId();
		char action   = master_msg.getAction();
		char response = action;

		if (processId < 0) return -1;


		if (action == 'S') {
			//suspend the process, send back sus message
			MigratableProcess process = pidMap.get(processId);
			process.suspend();
			writeProcess(process, processId);
			sendMessageToMaster(master_msg);
			return 0;
		}
		else if (action == 'R') {
			//start the new process, send back start msg
			MigratableProcess process = readProcess(processId);
			if (process != null) process.run();
			//we can just send the same message back
			pidMap.put(processId, process);
			sendMessageToMaster(master_msg);
			return 0;
		}
		else {
			System.err.println("Bad command: " + action);
			return -1;
		}

	}



	/**
	 * Starts a new slave host by opening up a socket and listening
	 * to stuff...sends a server socketaddress to the master so it
	 * knows where to send stuff
	 */
	private void startSlave() {
		//open the slave serversocket to communicate with the master
		try {
			slaveServerSocket = new ServerSocket(SLAVE_PORT);
			//slaveServerSocket.setSoTimeout(SOCK_TIMEOUT);
		} catch (Exception e) {
			System.err.println("Slave ServerSocket Creation failed!");
			e.printStackTrace();
			return;
		}
		///////////////////////////////////////////////

		while(true) {
			Socket master_sock = null;

			/*******************************************
			 * THIS BLOCK LISTENS FOR MASTER MESSAGES  *
		         * AND DOES STUFF			   *
			 *******************************************
			 */

			try {
				master_sock = slaveServerSocket.accept();
			} catch (SocketTimeoutException e) {
				master_sock = null;
			} catch (Exception e) {
				System.err.println("startSlave: Error creating the socket!");
			}


			if (master_sock != null) {
				//read the new message and do stuff
				SlaveMessage master_msg = readMessageFromMaster(master_sock);
				if (slave_do(master_msg) == -1) {
					System.err.println("Bad Message: " + master_msg.getProcessId() 
							  + " " +  master_msg.getAction());
				}
				try {
					master_sock.close();
				} catch (Exception e) {
					System.err.println("startSlave: Error closing the socket!");
				}
			}
		}
	}
	
	/** 
	 * Starts a new master host by opening up a master socket
	 * and monitoring stdin for new commands
	 */
	private void startMaster() throws IOException {
		//setup the input stream reader
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		//open the master serversocket to communicate with slaves
		try {
			master_sock           = new ServerSocket(MASTER_PORT);
			//set the timeout so accept() doesn't wait forever
			master_sock.setSoTimeout(SOCK_TIMEOUT);
		} catch (IOException e) {
			System.err.println("Master Socket creation failed!");
			e.printStackTrace();
			return;
		}
		//master_sock.configureBlocking(false);
		
		while(true) {
			//monitor stdin for new commands
			String input;
			String[] args;
			Socket slave_sock;

			/********************************************
			 * THIS BLOCK LISTENS FOR SLAVE MESSAGES    *
			 ********************************************
			 */

			//accept() creates a new socket for listening to the slave.  all we really 
			//need is the remote SocketAddress
			try {
				slave_sock = master_sock.accept();
			} catch (SocketTimeoutException e) {
				slave_sock = null;
			}

			if (slave_sock != null) {
				//create a new slave host and store the remote serversocket address
				//the remote server's socketaddress is communicated through an object stream
				InetAddress slave_iaddr         = slave_sock.getInetAddress();
				SlaveMessage slave_msg;

				if (slave_iaddr != null && iaddrMap.containsKey(slave_iaddr)) {
					slave_msg = readMessageFromSlave(slave_sock);
					master_do(slave_iaddr, slave_msg);
				}

				//add it to our current list of slaves
				//addNewSlave(slave_host, slave_iaddr);

			}

			////////////////////////////////////////////////////


			/******************************************
			 * THIS BLOCK LISTENS FOR USER INPUT      *
			 ******************************************
			 */

			try {
				input = reader.readLine();
				args = input.split(" ");
				if (args.length == 0)
					continue;
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
			
			if (args[0].equals("ps")) {
				printProcesses();
				continue;
			}
			else if (args[0].equals("quit")) {
				//TODO quit
				break;
			}

			///////////////////////////////////////////////////////


			/***********************************************************************
			 * This try/catch block find the specified class and instantiates it.  *
			 * If there are any failures, it lets the user know and goes back into *
			 * the input loop.                                                     *
			 ***********************************************************************
			 */

			MigratableProcess newProcess;
			try {
				Class<MigratableProcess> processClass = (Class<MigratableProcess>)(Class.forName(args[0]));
				
				Constructor<MigratableProcess> processConstructor = processClass.getConstructor(String[].class);
				
				newProcess = processConstructor.newInstance((Object[])args);
			
			} catch (ClassNotFoundException e) {
				//Couldn't link find that class. stupid user.
				System.out.println("Could not find class " + args[0]);
				continue;
			} catch (SecurityException e) {
				System.out.println("Security Exception getting constructor for " + args[0]);
				continue;
			} catch (NoSuchMethodException e) {
				System.out.println("Could not find proper constructor for " + args[0]);
				continue;
			} catch (IllegalArgumentException e) {
				System.out.println("Illegal arguments for " + args[0]);
				continue;
			} catch (InstantiationException e) {
				System.out.println("Instantiation Exception for " + args[0]);
				continue;
			} catch (IllegalAccessException e) {
				System.out.println("IIlegal access exception for " + args[0]);
				continue;
			} catch (InvocationTargetException e) {
				System.out.println("Invocation target exception for " + args[0]);
				continue;
			}

			//select a slave to send the process to
			if (currentProcessId >= 0) {
				int slaveId = currentProcessId % slave_list.size();
				sendProcessToSlave(slaveId, currentProcessId);
			}
			
			currentProcessId = newProcessId();

		}
	}

	public static void main(String[] argv) throws IOException {
		ProcessManager pm;
		//process argv
		System.out.println("starting");
		if (argv.length == 2) {
			if (argv[0] != "-c") {
				System.err.println("ERROR: Bad input");
				return;
			}
			//setup as slave
			pm = new ProcessManager(false, argv[1]);
			pm.startSlave();
		}
		else if (argv.length != 0) {
			System.err.println("ERROR: Incorrect # of arguments");
			return;
		}
		else {
			//setup as master PM
			pm = new ProcessManager(true, null);
			pm.startMaster();
		}
		
	}
}
