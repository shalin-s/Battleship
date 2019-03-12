/*
Battleships
OCS01 Final Project
by Shalin Shah

NOTE: For the music and sound effects to work properly, you must 
copy hitSound, missSound, gameBackgroundMusic, and menuBackgroundMusic (all .wav files) 
into the same folder as the .class files for this program.
*/

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.applet.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

public class Battleships
{
	//Constants:
	private static final int BOARD_SIZE = 10; //This number can be changed without causing any errors (as long as the board is big enough to hold the ships).
	private static final String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
	
		//shipSizes and shipNames are the ship complement. They can be changed without causing any errors, as long there are no null pointers in shipNames, and shipSizes and shipNames are the same length.
	private static final int[] shipSizes = {5, 4, 3, 3, 2}; 
	private static final String[] shipNames = {"carrier", "battleship", "submarine", "destroyer" , "patrol boat"};  //Each ship must have a different name (they are identified by name).
	
	private static final int portNum = 5000; //Can be changed to anything as long as portNum and portNum + 1 are valid, unused ports.
	private static final int chatPortNum = portNum + 1;

	//Game state variables:
	private static enum GameType {SERVER, CLIENT, VS_CPU};
	private static GameType gameType = null;

	private static enum GameState {SETUP, ONGOING, OVER}
	private static GameState gameState = GameState.SETUP;
	
	private static boolean isMyTurn = false;
	private static AttackCell [][] attackCells;
	private static DefenseCell [][] defenseCells;
	
	private static ArrayList <Ship> ships = new ArrayList <Ship>();
	private static int currentShip = 0;
	
	//Music and sound effects variables:
	private static boolean soundsOn = true;
	private static AudioClip hitSound;
	private static AudioClip missSound;
	private static AudioClip menuBackgroundMusic;
	private static AudioClip gameBackgroundMusic;
	
	//Objects that need to be accessed in multiple methods/inner classes (and so need to be declared here):
	private static JFrame window = new JFrame("Battleships");
	
	private static JPanel dashboard = new JPanel();
	private static JPanel dashboardTop = new JPanel();
	private static JPanel dashboardMiddle = new JPanel();
	private static JPanel dashboardBottom = new JPanel();
	
	private static JPanel attack = new JPanel(); 
	private static JPanel defense = new JPanel();
	private static JLabel defenseLabel = new JLabel(" ");
	private static JPanel chat = new JPanel();
	private static JPanel shipGrid = new JPanel();
	private static JLabel shipListLabel = new JLabel("Ships to place (in order):");
	private static JPanel grids = new JPanel();
	
	private static JLabel computerMessage = new JLabel();
	private static JLabel computerMessage2 = new JLabel("");
	private static JButton doneButton = new JButton("Done");
	
	private static ObjectOutputStream out;
	private static ObjectInputStream in;
	private static ObjectOutputStream chatOut;
	private static ObjectInputStream chatIn;
	private static JTextField chatToSend;
	private static JTextArea chatDisplay = new JTextArea(0, 80);
	private static JScrollPane chatScrollPane = new JScrollPane(chatDisplay);
	
	private static JFrame gameTypeDeterminationWindow = new JFrame("Battleships");
	
	//Variables only used if Server or playing against computer:
		//Game state variables:
	private static boolean placementDone = false;
	private static boolean clientPlacementDone = false;
	
		//GUI Objects:
	private static JFrame notificationWindow = new JFrame("Battleships");
	private static boolean notificationWindowInitialized = false;
	
	//Variables only used if Client:
		//GUI Objects:
	private static JFrame connectionSetup = new JFrame("Battleships");
	private static JLabel connectionSetupPrompt = new JLabel("Please enter IP address and port number of server (must be on same LAN).");
	private static JTextField IPAddressTextField = new JTextField(15);
	private static JTextField portTextField = new JTextField(6);
	private static JLabel connectionSetupError = new JLabel(" ");
	private static boolean connectionSetupInitialized = false;
	
	//End of variable definitions.
	
	private static void exitProgram (Exception e)
	{	
		if (gameState != GameState.OVER)
		{
			AudioClip[] sounds = {hitSound, missSound, gameBackgroundMusic, menuBackgroundMusic};
			
			for (AudioClip a: sounds)
			{
				try
				{
					a.stop();
				}
				
				catch (Exception exception)
				{
					
				}
			}
			
			e.printStackTrace();
			
			JFrame errorWindow = new JFrame("Battleships");
			errorWindow.setSize(500, 200);
			errorWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			errorWindow.setResizable(false);
			
			JPanel errorWindowTop = new JPanel();
			errorWindowTop.setLayout(new FlowLayout());
			JPanel errorWindowBottom = new JPanel();
			errorWindowBottom.setLayout(new FlowLayout());
			
			JLabel errorMessage = new JLabel();
			
			if (e instanceof SocketException)
			{
				errorMessage.setText("Fatal Error: Connection to opponent broken. Exiting program.");
			}
			
			else
			{
				errorMessage.setText("Fatal Error. Exiting Program.");
			}
			
			JButton okButton = new JButton("OK");
			okButton.addActionListener (new ActionListener()
			{
				public void actionPerformed (ActionEvent ae)
				{
					System.exit(0);
				}
			});
			
			errorWindowTop.add(errorMessage);
			errorWindowBottom.add(okButton);
			
			errorWindow.add(errorWindowTop, BorderLayout.NORTH);
			errorWindow.add(errorWindowBottom, BorderLayout.SOUTH);
			
			if (window != null)
			{
				window.setVisible(false);
			}
			
			errorWindow.setVisible(true);
		}
		
		else
		{
			System.exit(0);
		}
	}
	
	//The following four write/read methods are convenience methods.
	
	private static void writeInt(int i)
	{
		try
		{
			out.writeObject(i);
		}
		catch (Exception e)
		{
			exitProgram(e);
		}
	}
	
	private static int readInt ()
	{
		try
		{
			int toReturn = (Integer) in.readObject();
			return toReturn;
		}
		catch (Exception e)
		{
			exitProgram(e);
			return -1; //To keep the compiler happy
		}
	}
	
	private static void writeBoolean (boolean b)
	{
		try
		{
			out.writeObject(b);
		}
		catch (Exception e)
		{
			exitProgram(e);//left off here.
		}	
	}
	
	private static boolean readBoolean ()
	{
		try
		{
			boolean toReturn = (Boolean) in.readObject();
			return toReturn;
		}
		catch (Exception e)
		{
			exitProgram(e);
			return false; //To keep the compiler happy
		}
	}
	
	private static void addTextToChat (String text) //Adds text to the text chat box, and then scrolls down to the bottom if the scroll bar was originally at the bottom.
	{
		boolean autoscroll = false;
		if (chatScrollPane.getVerticalScrollBar().getValue() + chatScrollPane.getVerticalScrollBar().getModel().getExtent() == chatScrollPane.getVerticalScrollBar().getMaximum())
		{
			autoscroll = true;
		}
		
		chatDisplay.setRows(chatDisplay.getRows() + 1);
		chatDisplay.setText(chatDisplay.getText() + text + "\n");
		
		if (autoscroll)
		{
			chatDisplay.setCaretPosition(chatDisplay.getText().length());
		}	
	}
	
	private static abstract class Cell extends JButton implements ActionListener, Comparable<Cell>
	{
		protected int state = -1;
		protected int row;
		protected int col;
		
		public Cell (int r, int c)
		{
			super();
			setRow(r);
			setCol(c);
			updateText();
			this.setBackground(Color.BLUE);
			addActionListener(this);
		}
		
		private void updateText ()
		{
			this.setText(letters[col] + (row + 1));
		}
		
		public int getRow ()
		{
			return row;
		}
		
		public int getCol ()
		{
			return col;
		}
		
		public void setRow (int r)
		{
			if (r >= 0)
			{
				row = r;
			}
			
			updateText();
		}
		
		public void setCol (int c)
		{
			if (c >= 0)
			{
				col = c;
			}
			
			updateText();
		}
		
		public int getState ()
		{
			return state;
		}
		
		public boolean equals (Object other) //Cell's equals, compareTo, and toString are only used for debugging.
		{
			if (other.getClass() != getClass())
			{
				return false;
			}
			
			Cell otherCell = (Cell) other;
			
			if (otherCell.row == row && otherCell.col == col)
			{
				return true;
			}
			
			return false;
		}
		
		public int compareTo (Cell other) //Cell's equals, compareTo, and toString are only used for debugging.
		{
			if (row != other.row)
			{
				return (row - other.row);
			}
			
			else
			{
				return (col - other.col);
			}
		}
		
		public String toString () //Cell's equals, compareTo, and toString are only used for debugging.
		{
			return (this.getText());
		}
		
		public abstract void setState (int b);
		
		public abstract void actionPerformed (ActionEvent e);
	}
	
	private static class AttackCell extends Cell 
	{
		public static final int NOT_GUESSED = -1;
		public static final int MISS = 0;
		public static final int HIT = 1;
		
		public AttackCell (int r, int c)
		{
			super (r, c);
		}
		
		public void setState (int b) //-1 is not guessed, 0 is miss, 1 is hit.
		{
			if (b > 1 || b < -1)
			{
				return;
			}
			
			state = b;
			switch (state)
			{
				case NOT_GUESSED:
					this.setBackground(Color.BLUE);
					break;
				case MISS:
					this.setBackground(Color.WHITE);
					break;
				case HIT:
					this.setBackground(Color.RED);
					break;
				default:
					return;
			}
		}
		
		public void actionPerformed (ActionEvent e)
		{
			if ((state != NOT_GUESSED) || (!isMyTurn)|| (gameState != GameState.ONGOING))
			{
				return;
			}
			
			try
			{
				if (soundsOn)
				{
					hitSound.stop();
					missSound.stop();
				}
				
				computerMessage2.setText(" ");
				writeInt(row);
				writeInt(col);
				boolean hit = readBoolean();
				int ship = readInt();
				boolean gameOver = readBoolean();
	
				if (hit)
				{
					setState(HIT);
					computerMessage2.setText(getText() + " is a hit!");
					if (soundsOn)
					{
						hitSound.play();
					}
					
					if (ship != -1)
					{
						computerMessage2.setText(computerMessage2.getText() + " You have sunk your opponent's " + shipNames[ship] + " of length " + shipSizes[ship] + "!");
						
						for (int i = 0; i < shipGrid.getComponents().length; i++)
						{
							Component c = shipGrid.getComponent(i);
							if (!(c instanceof JPanel))
							{
								continue;
							}
							JPanel panel = (JPanel) c;
							Component toCheck = panel.getComponent(0);
							
							if (!(toCheck instanceof JLabel))
							{
								continue;
							}
							JLabel label = (JLabel) toCheck;
							if (label.getText().equals(shipNames[ship]))
							{
								shipGrid.remove(i);
								shipGrid.remove(i);
								GridLayout g = (GridLayout) shipGrid.getLayout();
								g.setRows(g.getRows() - 1);
								break;
							}	
						}
					}
				}
				
				else
				{
					setState(MISS);
					computerMessage2.setText(getText() + " is a miss.");
					if (soundsOn)
					{
						missSound.play();
					}
				}
				
				
				
				if (gameOver)
				{
					computerMessage.setText("You win.");
					computerMessage2.setText("Game Over.");
					gameState = GameState.OVER;
					return;
				}
				
				isMyTurn = false;
				computerMessage.setText("Opponent's turn.");
				new GuessProcessor().start();
			}	
			
			catch (Exception exception)
			{
				exitProgram(exception);
			}
		}
	}
		
	private static class DefenseCell extends Cell
	{
		public static final int EMPTY = -1;
		public static final int MISS = 0;
		public static final int UNHIT_SHIP = 1;
		public static final int HIT = 2;
		public static final int FIXED_FOR_SHIP_PLACEMENT = 3;
		
		private Ship myShip;
		
		public DefenseCell (int r, int c)
		{
			super(r, c);
		}
		
		public void setShip (Ship s)
		{
			myShip = s;
		}
		
		public Ship getShip ()
		{
			return myShip;
		}

		public void setState (int b)
		{
			if (b > 3 || b < -1)
			{
				return;
			}
			
			state = b;
			switch (state)
			{
				case EMPTY:
					this.setBackground(Color.BLUE);
					break;
				case MISS:
					this.setBackground(Color.WHITE);
					break;
				case UNHIT_SHIP:
					this.setBackground(Color.LIGHT_GRAY);
					break;
				case HIT:
					this.setBackground(Color.RED);
					break;
				default:
					return;
			}
		}

		public void actionPerformed (ActionEvent e)
		{
			if (gameState != GameState.SETUP)
			{
				return;
			}
			
			if (state == UNHIT_SHIP)
			{
				setState(EMPTY);
			}
			
			else if (state == EMPTY)
			{
				setState(UNHIT_SHIP);
			}
		}
	}
	
	private static ArrayList <DefenseCell> getOccupiedDefenseCells ()
	{
		ArrayList <DefenseCell> occupied = new ArrayList <DefenseCell>();
		for (int r = 0; r < defenseCells.length; r++)
		{
			for (int c = 0; c < defenseCells[0].length; c++)
			{
				if (defenseCells[r][c].getState() == DefenseCell.UNHIT_SHIP)
				{
					occupied.add(defenseCells[r][c]);
				}
			}
		}
		
		return occupied;
	}

	private static class Ship
	{
		private DefenseCell[] locations;
		private String name;
		
		public Ship (String n, int size, ArrayList<DefenseCell> cells) throws Exception
		{
			name = n;
			
			if (size != cells.size() || size <= 0 || cells.size () == 0)
			{
				throw new Exception ();
			}
			
			boolean sameRow = true;
			boolean sameCol = true;
			
			for (int i = 1; i < cells.size(); i++)
			{
				if (cells.get(i).getRow() != cells.get(i-1).getRow())
				{
					sameRow = false;
				}
				
				if (cells.get(i).getCol() != cells.get(i-1).getCol())
				{
					sameCol = false;
				}
			}
			
			if (!(sameRow || sameCol))
			{
				throw new Exception();
			}
			
			DefenseCell[] cellsArray = new DefenseCell[cells.size()]; 
			cells.toArray(cellsArray);
			Arrays.sort(cellsArray);
			
			if (sameRow)
			{
				for (int i = 1; i < cellsArray.length; i++)
				{
					if (cellsArray[i].getCol() != cellsArray[0].getCol() + i)
					{
						throw new Exception();
					}
				}
			}
			
			else
			{
				for (int i = 1; i < cellsArray.length; i++)
				{
					if (cellsArray[i].getRow() != cellsArray[0].getRow() + i)
					{
						throw new Exception();
					}
				}
			}
			
			locations = cellsArray;
			
			for (DefenseCell d: locations)
			{
				d.setShip(this);
			}
		}
		
		public DefenseCell[] getLocations ()
		{
			return locations;
		}
		
		public String getName ()
		{
			return name;
		}
		
		public boolean isInShip (DefenseCell d)
		{
			for (DefenseCell defenseCell: locations)
			{
				if (d == defenseCell)
				{
					return true;
				}
			}
			
			return false;
		}
		
		public int getIndex ()
		{
			for (int i = 0; i < shipNames.length; i++)
			{
				if (getName().equals(shipNames[i]))
				{
					return i;
				}
			}
			
			return -1;
		}
		
		public boolean isSunk ()
		{
			for (DefenseCell d: locations)
			{
				if (d.getState() == DefenseCell.UNHIT_SHIP)
				{
					return false;
				}
			}
			
			return true;
		}
		
		public void hit (DefenseCell d)
		{
			if (isInShip(d))
			{
				d.setState(DefenseCell.HIT);
			}
		}
	}
	
	private static class GuessProcessor extends Thread //Waits for the opponent to guess and then processes the opponent's guess.
	{
		public void run() 
		{
			try 
			{
				int guessRow = readInt();
				int guessCol = readInt();
				
				if (guessRow >= defenseCells.length || guessRow < 0 || guessCol >= defenseCells[0].length || guessCol < 0) //safety measure
				{
					isMyTurn = true;
					computerMessage.setText("Your Turn.");
					return;
				}
					
				Ship s = defenseCells[guessRow][guessCol].getShip();
				if (s != null)
				{
					s.hit(defenseCells[guessRow][guessCol]);
					writeBoolean(true);
					
					if (s.isSunk())
					{
						for (DefenseCell defenseCell: s.getLocations())
						{
							defenseCell.setState(DefenseCell.EMPTY);
						}
						
						ships.remove(s);
						computerMessage2.setText("Your " + s.getName() + " of length " + s.getLocations().length + " has been sunk!");
						writeInt(s.getIndex());		

						if (ships.isEmpty())
						{
							computerMessage.setText("You lose.");
							computerMessage2.setText("Game Over.");
							writeBoolean(true);
							gameState = GameState.OVER;
							return;
						}
						
						else
						{
							writeBoolean(false);
						}
					}
					
					else
					{
						writeInt(-1);
						writeBoolean(false);
					}
				}
				
				else
				{
					defenseCells[guessRow][guessCol].setState(DefenseCell.MISS);
					writeBoolean(false);
					writeInt(-1);
					writeBoolean(false);
				}
				
				isMyTurn = true;
				computerMessage.setText("Your Turn.");
			}
			
			catch (Exception e)
			{
				exitProgram(e);
			}
		}
	}
	
	private static class Chat extends Thread
	{
		public void run() 
		{
			try 
			{
				while (window.isVisible())
				{
					String toAdd = (String) chatIn.readObject();
					addTextToChat("OPPONENT: " + toAdd);
				}
			}
			
			catch (Exception e)
			{
				exitProgram(e);
			}
		}
	}

	private static void showGameTypeDetermination ()
	{
		gameTypeDeterminationWindow.setSize(525, 125);
		gameTypeDeterminationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gameTypeDeterminationWindow.setResizable(false);
		
		JLabel welcomeLabel = new JLabel("Welcome to Battleships.");
		JPanel welcomePanel = new JPanel();
		welcomePanel.setLayout(new FlowLayout());
		
		JPanel gameTypeButtons = new JPanel();
		gameTypeButtons.setLayout(new FlowLayout());
		
		JButton serverButton = new JButton("Play as Server");
		serverButton.addActionListener(new ActionListener()
		{
			public void actionPerformed (ActionEvent e)
			{
				gameType = GameType.SERVER;
				gameTypeDeterminationWindow.setVisible(false);
				initializeConnection();
			}
		});
		
		JButton clientButton = new JButton("Play as Client");
		clientButton.addActionListener(new ActionListener()
		{
			public void actionPerformed (ActionEvent e)
			{
				gameType = GameType.CLIENT;
				gameTypeDeterminationWindow.setVisible(false);
				initializeConnection();
			}
		});
		
		JButton computerButton = new JButton("Play against CPU");
		computerButton.addActionListener(new ActionListener()
		{
			public void actionPerformed (ActionEvent e)
			{
				gameType = GameType.VS_CPU;
				gameTypeDeterminationWindow.setVisible(false);
				initializeConnection();
			}
		});
		
		welcomePanel.add(welcomeLabel);
		
		gameTypeButtons.add(serverButton);
		gameTypeButtons.add(clientButton);
		gameTypeButtons.add(computerButton);
		
		gameTypeDeterminationWindow.add(welcomePanel, BorderLayout.NORTH);
		gameTypeDeterminationWindow.add(gameTypeButtons, BorderLayout.CENTER);
		
		if (!soundsOn)
		{
			JPanel errorPanel = new JPanel();
			errorPanel.setLayout(new FlowLayout());
			errorPanel.add(new JLabel("Music/Sound Effects failed to load. Game will run with sound disabled."));
			gameTypeDeterminationWindow.add(errorPanel, BorderLayout.SOUTH);
		}
		
		gameTypeDeterminationWindow.setVisible(true);
	}

	private static void initializeConnection ()
	{	
		if (gameType == GameType.SERVER)
		{
			if (notificationWindowInitialized)
			{
				notificationWindow.setVisible(true);
				return;
			}
			
			String myAddress = "";
			try
			{
				myAddress = InetAddress.getLocalHost().getHostAddress();
			}
			catch (Exception e)
			{
				exitProgram(e);
			}
			
			notificationWindow.setSize(525, 150);
			notificationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			notificationWindow.setResizable(false);
			notificationWindow.setLayout(new GridLayout(4,1));
			
			JLabel notification1 = new JLabel("Please inform client (must be on same LAN) to enter connection information as follows.");
			JLabel notification2 = new JLabel("IP address: " + myAddress);
			JLabel notification3 = new JLabel("Port: " + portNum);
			JLabel notification4 = new JLabel("Game will start once client has connected.");
			
			notificationWindow.add(notification1);
			notificationWindow.add(notification2);
			notificationWindow.add(notification3);
			notificationWindow.add(notification4);
			
			notificationWindow.setVisible(true);
			notificationWindowInitialized = true;
			
			new WaiterForClientConnection().start();	
		}
		
		else if (gameType == GameType.CLIENT)
		{
			JButton backButton = new JButton("Back");
			backButton.addActionListener(new ActionListener()
			{
				public void actionPerformed (ActionEvent e)
				{
					connectionSetup.setVisible(false);
					notificationWindow.setVisible(false);
					gameType = null;
					showGameTypeDetermination();
				}
			});
			
			if (connectionSetupInitialized)
			{
				connectionSetup.setVisible(true);
				return;
			}
			
			connectionSetup.setSize(600, 125);
			connectionSetup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			connectionSetup.setResizable(false);
			
			JPanel connectionSetupInput = new JPanel();
			connectionSetupInput.setLayout(new FlowLayout());
			
			JButton connectionSetupDoneButton = new JButton("Done");
			connectionSetupDoneButton.addActionListener(new ActionListener()
			{
				public void actionPerformed (ActionEvent e)
				{
					try
					{
						Socket socket1 = new Socket(IPAddressTextField.getText().trim(), Integer.parseInt(portTextField.getText().trim()));
						Socket socket2 = new Socket(IPAddressTextField.getText().trim(), Integer.parseInt(portTextField.getText().trim()) + 1);
						
						in = new ObjectInputStream(socket1.getInputStream());
						chatIn = new ObjectInputStream(socket2.getInputStream());
						out = new ObjectOutputStream(socket1.getOutputStream());
						chatOut = new ObjectOutputStream(socket2.getOutputStream());
					}
					
					catch (Exception exception)
					{
						connectionSetupError.setText("Error connecting to server. Please make sure the information you entered is correct and try again.");
						return;
					}
					
					connectionSetup.setVisible(false);
					initializeShipPlacement();
				}
			});
			
			connectionSetupInput.add(new JLabel("IP Address:"));
			connectionSetupInput.add(IPAddressTextField);
			connectionSetupInput.add(new JLabel("Port:"));
			connectionSetupInput.add(portTextField);
			connectionSetupInput.add(connectionSetupDoneButton);
			connectionSetupInput.add(backButton);
			
			connectionSetup.add(connectionSetupPrompt, BorderLayout.NORTH);
			connectionSetup.add(connectionSetupInput, BorderLayout.CENTER);
			connectionSetup.add(connectionSetupError, BorderLayout.SOUTH);
			
			connectionSetup.setVisible(true);
			connectionSetupInitialized = true;
		}
		
		else if (gameType == GameType.VS_CPU)
		{
			new WaiterForClientConnection().start();
			new CPUPlayer().start();
		}
	}

	private static class WaiterForClientConnection extends Thread
	{
		public void run ()
		{
			try
			{
				ServerSocket serverSocket = new ServerSocket(portNum);
				ServerSocket serverSocket2 = new ServerSocket(chatPortNum);
				Socket socket1 = serverSocket.accept();
				Socket socket2 = serverSocket2.accept();
				
				out = new ObjectOutputStream(socket1.getOutputStream());
				chatOut = new ObjectOutputStream(socket2.getOutputStream());
				in = new ObjectInputStream(socket1.getInputStream());
				chatIn = new ObjectInputStream(socket2.getInputStream());
			}
			
			catch (Exception e)
			{
				exitProgram(e);
			}
			
			notificationWindow.setVisible(false);
			initializeShipPlacement();
		}
	}

	private static void initializeShipPlacement ()
	{
		window.setSize(1400, 910);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setMinimumSize(new Dimension(950, 700));
		
		window.addComponentListener(new ComponentAdapter()
		{
			public void componentResized (ComponentEvent e)
			{
				int newMaxHeight = (int)(0.95 * ((double) window.getSize().getHeight() - dashboard.getSize().getHeight() - chat.getSize().getHeight() - 60) / BOARD_SIZE);
				
				if (gameState == GameState.SETUP)
				{
					int newMaxWidth = (int)(((window.getSize().getWidth()) - 3.0 * ((FlowLayout) grids.getLayout()).getHgap() - 2.0 * shipGrid.getComponent(0).getSize().getWidth())/ (double) BOARD_SIZE);
					int newSideLength = Math.min(newMaxHeight, newMaxWidth);
					Dimension newSize = new Dimension(newSideLength, newSideLength);
					
					for (int i = 0; i < BOARD_SIZE; i++)
					{
						for (int j = 0; j < BOARD_SIZE; j++)
						{
							defenseCells[i][j].setPreferredSize(newSize);
							defenseCells[i][j].setSize(newSize);
						}
					}
				}
				
				else
				{
					int newMaxWidth = (int)(((window.getSize().getWidth()) - 4 * ((FlowLayout) grids.getLayout()).getHgap() - 2.0 * shipGrid.getComponent(0).getSize().getWidth())/(BOARD_SIZE * 2.0));
					int newSideLength = Math.min(newMaxHeight, newMaxWidth);
					Dimension newSize = new Dimension(newSideLength, newSideLength);
					
					for (int i = 0; i < BOARD_SIZE; i++)
					{
						for (int j = 0; j < BOARD_SIZE; j++)
						{
							attackCells [i][j].setPreferredSize(newSize);
							attackCells [i][j].setSize(newSize);
							defenseCells[i][j].setPreferredSize(newSize);
							defenseCells[i][j].setSize(newSize);
						}
					}
				}
			}
		});
		
		window.addWindowStateListener(new WindowAdapter() //Disables maximization of window (to be accurate, immediately un-maximizes the window if the user maximizes it), as maximization would mess up the grid sizes.
		{
			public void windowStateChanged (WindowEvent e)
			{
				if (e.getNewState() == JFrame.MAXIMIZED_BOTH)
				{
					window.setExtendedState(JFrame.NORMAL);
				}
			}
		});
		
		dashboard.setLayout(new GridLayout(3, 1));

		dashboardTop.setLayout(new FlowLayout());	
		dashboardMiddle.setLayout(new FlowLayout());	
		dashboardBottom.setLayout(new FlowLayout());
		
		JLabel dashboardTopBanner = new JLabel();
		
		if (gameType == GameType.SERVER)
		{
			dashboardTopBanner.setText("Battleships: playing as server");
		}
		
		else if (gameType == GameType.CLIENT)
		{
			dashboardTopBanner.setText("Battleships: playing as client");
		}
		
		else if (gameType == GameType.VS_CPU)
		{
			dashboardTopBanner.setText("Battleships: playing against CPU");
		}
		
		computerMessage.setText("Please choose a location for your " + shipNames[0] + " of length " + shipSizes[0] + ".");
		computerMessage2.setText(" ");
		
		dashboardTop.add(dashboardTopBanner);
		dashboardMiddle.add(computerMessage);
		dashboardBottom.add(computerMessage2);
		
		dashboard.add(dashboardTop);
		dashboard.add(dashboardMiddle);
		dashboard.add(dashboardBottom);
		
		doneButton.addActionListener(new ActionListener()
		{
			public void actionPerformed (ActionEvent e)
			{
				if (placementDone)
				{
					return;
				}
				
				ArrayList<DefenseCell> occupied = getOccupiedDefenseCells();
				
				try
				{
					Ship toAdd = new Ship(shipNames[currentShip], shipSizes[currentShip], occupied);
					ships.add(toAdd);
				}
				
				catch (Exception exception)
				{
					computerMessage2.setText("Invalid ship location, please try again.");
					return;
				}
				
				computerMessage2.setText("");
				
				for (DefenseCell d: occupied)
				{
					d.setState(DefenseCell.FIXED_FOR_SHIP_PLACEMENT);
				}
				
				currentShip++;
				
				if (currentShip >= shipSizes.length)
				{
					if (gameType == GameType.SERVER || gameType == GameType.VS_CPU)
					{
						if (clientPlacementDone)
						{
							writeBoolean(true);
							initializeMainGame();
							return;
						}
						
						else
						{
							computerMessage.setText("Waiting for opponent to finish placing ships.");
							dashboardMiddle.remove(doneButton);
							placementDone = true;
						}
					}
					
					else if (gameType == GameType.CLIENT)
					{
						writeBoolean(true);
						computerMessage.setText("Waiting for opponent to finish placing ships.");
						dashboardMiddle.remove(doneButton);
						new WaiterForServerPlacement().start();
					}
				}
				
				else
				{
					computerMessage.setText("Please choose a location for your " + shipNames[currentShip] + " of length " + shipSizes[currentShip] + ".");
				}
			}
		});
		
		dashboardMiddle.add(doneButton);
		
		JPanel shipList = new JPanel();
		shipList.setLayout(new BorderLayout());
		JPanel shipListTitle = new JPanel();
		shipListTitle.setLayout(new FlowLayout());
		shipListLabel = new JLabel("Ships to place (in order):");
		shipListTitle.add(shipListLabel);
		
		shipGrid.setLayout(new GridLayout(shipSizes.length + 1, 2));
		
		for (int i = -1; i < shipSizes.length; i++)
		{
			JPanel namePanel = new JPanel();
			namePanel.setLayout(new FlowLayout());
			namePanel.setBorder(LineBorder.createBlackLineBorder());
			JLabel name;
			if (i >= 0)
			{
				name = new JLabel(shipNames[i]);
			}
			else
			{
				name = new JLabel("Ship:");
			}
			namePanel.add(name);
			shipGrid.add(namePanel);
			
			JPanel sizePanel = new JPanel();
			FlowLayout sizeLayout = new FlowLayout();
			sizeLayout.setHgap(0);
			sizePanel.setLayout(sizeLayout);
			sizePanel.setBorder(LineBorder.createBlackLineBorder());
			if (i >= 0)
			{
				for (int j = 0; j < shipSizes[i]; j++)
				{
					JPanel temp = new JPanel();
					temp.setBorder(LineBorder.createBlackLineBorder());
					sizePanel.add(temp);
				}
			}
			else
			{
				sizePanel.add(new JLabel("Size:"));
			}
			
			shipGrid.add(sizePanel);		
		}
		
		shipList.add(shipListTitle, BorderLayout.NORTH);
		shipList.add(shipGrid, BorderLayout.CENTER);
		
		JPanel defenseGrid = new JPanel();
		defenseGrid.setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
		defenseCells = new DefenseCell[BOARD_SIZE][BOARD_SIZE];
		for (int i = 0; i < defenseCells.length; i++)
		{
			for (int j = 0; j < defenseCells[0].length; j++)
			{
				defenseCells[i][j] = new DefenseCell(i, j);
				defenseCells[i][j].setMargin(new Insets(0, 0, 0, 0));
				defenseGrid.add(defenseCells[i][j]);
			}
		}
		JPanel defenseLabelPanel = new JPanel();
		defenseLabelPanel.setLayout(new FlowLayout());
		defenseLabelPanel.add(defenseLabel);
		defense.setLayout(new BorderLayout());
		defense.add(defenseLabelPanel, BorderLayout.NORTH);
		defense.add(defenseGrid, BorderLayout.CENTER);
		
		chatToSend = new JTextField(60);
		JButton sendButton = new JButton("Send");
		ActionListener messageSender = new ActionListener()
		{
			public void actionPerformed (ActionEvent e)
			{
				try
				{
					chatOut.writeObject(chatToSend.getText());
				}
				
				catch (Exception exception)
				{
					exitProgram(exception);
				}
				
				addTextToChat("ME:" + chatToSend.getText());
				chatToSend.setText("");
			}
		};
		sendButton.addActionListener(messageSender);
		chatToSend.addActionListener(messageSender);
		
		JPanel chatControls = new JPanel();
		chatControls.setLayout(new FlowLayout());
		chatControls.add(chatToSend);
		chatControls.add(sendButton);
		
		chatDisplay.setEditable(false);
		DefaultCaret caret = (DefaultCaret) chatDisplay.getCaret();
		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		
		chat.setLayout(new GridLayout(3, 1));
		chatScrollPane.setPreferredSize(new Dimension(window.getWidth(), 70));
		JPanel chatTopPanel = new JPanel();
		chatTopPanel.setLayout(new FlowLayout());
		JLabel chatLabel = new JLabel("Text Chat:");
		chatTopPanel.add(chatLabel);
		chat.add(chatTopPanel);
		chat.add(chatScrollPane);
		chat.add(chatControls);
		
		FlowLayout temp = new FlowLayout();
		temp.setHgap(60);
		grids.setLayout(temp);
		grids.add(shipList);
		grids.add(defense);
		
		window.add(dashboard, BorderLayout.NORTH);
		window.add(grids, BorderLayout.CENTER);
		window.add(chat, BorderLayout.SOUTH);
		
		if (gameType == GameType.SERVER || gameType == GameType.VS_CPU)
		{
			new WaiterForClientPlacement().start();
		}
		
		if (soundsOn)
		{
			menuBackgroundMusic.stop();
			gameBackgroundMusic.loop();
		}
		
		window.setVisible(true);

		new Chat().start();
	}

	private static class WaiterForServerPlacement extends Thread
	{
		public void run ()
		{
			readBoolean();
			initializeMainGame();
		}
	}

	private static class WaiterForClientPlacement extends Thread
	{
		public void run ()
		{
			try
			{
				in.readObject();		
			}
			
			catch (Exception e)
			{
				exitProgram(e);
			}
			
			clientPlacementDone = true;
			
			if (placementDone)
			{
				writeBoolean(true);
				initializeMainGame();
			}
		}
	}
	
	private static void initializeMainGame ()
	{
		JPanel attackGrid = new JPanel();
		attackGrid.setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
		attackCells = new AttackCell[BOARD_SIZE][BOARD_SIZE];
		for (int i = 0; i < attackCells.length; i++)
		{
			for (int j = 0; j < attackCells[0].length; j++)
			{
				attackCells[i][j] = new AttackCell(i, j);
				attackCells[i][j].setMargin(new Insets(0, 0, 0, 0));
				attackGrid.add(attackCells[i][j]);
			}
		}
		JPanel attackLabelPanel = new JPanel();
		JLabel attackLabel = new JLabel("Attack:");
		attackLabelPanel.setLayout(new FlowLayout());
		attackLabelPanel.add(attackLabel);
		attack.setLayout(new BorderLayout());
		attack.add(attackLabelPanel, BorderLayout.NORTH);
		attack.add(attackGrid, BorderLayout.CENTER);
		
		defenseLabel.setText("Defense:");
		for (Ship s: ships)
		{
			DefenseCell[] locations = s.getLocations();
			for (DefenseCell d: locations)
			{
				d.setState(DefenseCell.UNHIT_SHIP);
			}
		}
		
		dashboardMiddle.remove(doneButton);
		computerMessage2.setText("");
		
		if (gameType == GameType.SERVER || gameType == GameType.VS_CPU)
		{
			int random = (int)(Math.random() * 2);
			
			if (random == 1)
			{
				computerMessage.setText("Your turn.");
				isMyTurn = true;
			}
			
			else
			{
				computerMessage.setText("Opponent's turn.");
				writeInt(-1);
				writeInt(-1);
				isMyTurn = false;
				new GuessProcessor().start();
			}
		}
		
		else if (gameType == GameType.CLIENT)
		{
			isMyTurn = false;
			computerMessage.setText("Opponent's turn.");
			new GuessProcessor().start();
		}
		
		shipListLabel.setText("Remaining enemy ships:");
		grids.add(attack, 1);
		
		gameState = GameState.ONGOING;
		window.getComponentListeners()[0].componentResized(new ComponentEvent(window, ComponentEvent.COMPONENT_RESIZED));
	}
	
	private static class CPUPlayer extends Thread
	{
		private ArrayList <Ship> computerShips = new ArrayList<Ship>();
		private DefenseCell[][] computerDefenseCells = new DefenseCell[BOARD_SIZE][BOARD_SIZE];

		private int direction = 0; //0 is up, 1 is right, 2 is down, 3 is left
		private int distanceFromOrigin = 1;
		private boolean[] directionEliminated = {false, false, false, false};
		private int[] origin;
		private boolean ontoShip = false;

		int[][] neighborCounts = new int[BOARD_SIZE][BOARD_SIZE]; //number of guessed neighbors (up, down, left right, not diagonal) for each cell, or -1 if cell already guessed.
		ArrayList<int[]> nextOrigins = new ArrayList<int[]>(); //a list of all locations so far that were hit while trying to sink a different ship (can occur when two ships are next to each other). After sinking the current ship, origin will be set to the first element in nextOrigins (assuming nextOrigins is not empty).
		
		/*
		 * Explanation of CPUPlayer algorithm:
		 * 
		 * Placement:
		 * 
		 * For each ship, CPUPlayer first decides randomly whether the ship should be in the same row or same column, and then chooses a random valid location for the ship.
		 * 
		 * Playing:
		 * 
		 * Repeat the following on each turn:
		 * 
		 * If not ontoShip
		 * 	- Guess a random cell from the list of the cells with the least neighbors. If it is a hit, set origin to the guessed cell, and set ontoShip to true.
		 * 
		 * If ontoShip
		 * 	- If the current direction has been eliminated, choose a random direction that has not been eliminated,and set distance from origin to 1.
		 * 	- take the cell that is in the current direction (direction) and the current distance from the origin (distanceFromOrigin).
		 * 		-If it is already guessed, or if it is off the board
		 * 			- If it is not off the board, and it was a hit, and the ship associated with the cell is not sunk
		 * 				-Increment distanceFromOrigin and start over from "If ontoShip".
		 * 			- If it was a miss, or it is off the board
		 * 				-Eliminate the current direction, and if distanceFromOrigin > 1, set direction to the opposite of the current direction. Then start over from "If ontoShip".
		 * 		-If it has not already been guessed, guess it.
		 * 			- If it is a hit
		 * 				- If the ship (must be the same one as at the origin) is sunk, set ontoShip to false.
		 * 				- Otherwise increment distanceFromOrigin.
		 * 			- If it is a miss
		 * 				- Eliminate the current direction, and if distanceFromOrigin > 1, set direction to the opposite of the current direction.
		 */
		
		public CPUPlayer ()
		{
			for (int i = 0; i < computerDefenseCells.length; i++)
			{
				for (int j = 0; j < computerDefenseCells[0].length; j++)
				{
					computerDefenseCells[i][j] = new DefenseCell(i, j);
				}
			}
		}
		
		private void placeShips ()
		{
			for (int i = 0; i < shipSizes.length; i++)
			{
				boolean done = false;
				
				outerLoop:
				while (!done)
				{
					ArrayList<DefenseCell> toAdd = new ArrayList <DefenseCell>();
					boolean sameRow = (((int) (Math.random() * 2)) == 1);
					
					try
					{
						if (sameRow)
						{
							int row =  (int) (Math.random() * computerDefenseCells.length);
							int startCol = (int) (Math.random() * (computerDefenseCells[0].length - shipSizes[i] + 1));
							
							for (int c = startCol; c < (startCol + shipSizes[i]); c++)
							{
								toAdd.add(computerDefenseCells[row][c]);
							}
						}
						
						else
						{
							int col =  (int) (Math.random() * computerDefenseCells[0].length);
							int startRow = (int) (Math.random() * (computerDefenseCells.length - shipSizes[i] + 1));
							
							for (int r = startRow; r < (startRow + shipSizes[i]); r++)
							{
								toAdd.add(computerDefenseCells[r][col]);
							}
						}
						
						for (DefenseCell d: toAdd)
						{
							if (d.getState() == DefenseCell.UNHIT_SHIP)
							{
								continue outerLoop;
							}
						}
						
						computerShips.add(new Ship(shipNames[i], shipSizes[i], toAdd));
						
						for (DefenseCell d: toAdd)
						{
							d.setState(DefenseCell.UNHIT_SHIP);
						}
						
						done = true;
					}
					
					catch (Exception e)
					{
						
					}	
				}	
			}
		}
		
		private int[] nextGuess ()
		{
			int[] guess;
			
			if (!ontoShip)
			{
				ArrayList<int[]> leastNeighbors = new ArrayList <int[]>();
				for (int i = 0; i <= 4; i++)
				{
					for (int r = 0; r < neighborCounts.length; r++)
					{
						for (int c = 0; c < neighborCounts[0].length; c++)
						{
							if (neighborCounts[r][c] == i)
							{
								int[] temp = {r, c};
								leastNeighbors.add(temp);
							}
						}
					}
					
					if (!leastNeighbors.isEmpty())
					{
						break;
					}
				}
				
				guess = leastNeighbors.get((int)(Math.random() * leastNeighbors.size()));
			}
			
			else
			{	
				while (true)
				{
					while (directionEliminated[direction])
					{
						direction = (int) (Math.random() * 4);
						distanceFromOrigin = 1;
					}
					
					int[] temp = {origin[0], origin[1]};
					guess = temp;
					
					switch (direction)
					{
						case 0:
							guess[0] -= distanceFromOrigin;
							break;
						case 1:
							guess[1] += distanceFromOrigin;
							break;
						case 2:
							guess[0] += distanceFromOrigin;
							break;
						case 3:
							guess[1] -= distanceFromOrigin;
							break;
					}
					
					if (guess[0] >= computerDefenseCells.length || guess[0] < 0 || guess[1] >= computerDefenseCells[0].length || guess[1] < 0)
					{
						directionEliminated[direction] = true;
						if (distanceFromOrigin > 1)
						{
							direction = (direction + 2) % 4;
							distanceFromOrigin = 1;
						}
						continue;
					}
		
					if (neighborCounts[guess[0]][guess[1]] == -1)
					{
						Ship s = defenseCells[guess[0]][guess[1]].getShip();
						boolean wasHit = (s != null);
						
						if (wasHit && !s.isSunk())
						{
							distanceFromOrigin++;
						}
						
						else
						{
							directionEliminated[direction] = true;
							if (distanceFromOrigin > 1)
							{
								direction = (direction + 2) % 4;
								distanceFromOrigin = 1;
							}
						}
						
						continue;
					}
					
					break;
				}
			}

			neighborCounts [guess[0]][guess[1]] = -1;
			
			for (int r = guess[0] - 1; r <= guess[0] + 1; r += 2)
			{
				try
				{
					if (neighborCounts[r][guess[1]] != -1)
					{
						neighborCounts[r][guess[1]]++;
					}
				}
				catch (Exception e)
				{
					
				}
			}
			
			for (int c = guess[1] - 1; c <= guess[1] + 1; c += 2)
			{
				try
				{
					if (neighborCounts[guess[0]][c] != -1)
					{
						neighborCounts[guess[0]][c]++;
					}
				}
				catch (Exception e)
				{
					
				}
			}

			return guess;
		}
		
		public void run ()
		{
			try
			{
				Socket socket1 = new Socket("localhost", portNum);
				Socket socket2 = new Socket("localhost", chatPortNum);
				
				ObjectOutputStream computerOut = new ObjectOutputStream(socket1.getOutputStream());
				ObjectOutputStream computerOut2 = new ObjectOutputStream(socket2.getOutputStream());
				ObjectInputStream computerIn = new ObjectInputStream(socket1.getInputStream());
				ObjectInputStream computerIn2 = new ObjectInputStream(socket2.getInputStream());

				placeShips();
				computerOut.writeObject(true);
				computerIn.readObject();
				
				while (true)
				{
					int r = (Integer) computerIn.readObject();
					int c = (Integer) computerIn.readObject();
					boolean guessValid = true;
					
					if (r >= computerDefenseCells.length || r < 0 || c >= computerDefenseCells[0].length || c < 0)
					{
						guessValid = false;
					}
					
					if (guessValid)
					{
						Ship s = computerDefenseCells[r][c].getShip();
						
						if (s != null)
						{
							s.hit(computerDefenseCells[r][c]);
							computerOut.writeObject(true);
							
							if (s.isSunk())
							{
								computerShips.remove(s);
								computerOut.writeObject(s.getIndex());
								
								
								if (computerShips.size() == 0)
								{
									computerOut.writeObject(true);
									return;
								}
								
								else
								{
									computerOut.writeObject(false);
								}
							}
							
							else
							{
								computerOut.writeObject(-1);
								computerOut.writeObject(false);
							}	
						}
						
						else
						{
							computerOut.writeObject(false);
							computerOut.writeObject(-1);
							computerOut.writeObject(false);
						}
					}
					
					Thread.sleep(1000);
					
					int[] guess = nextGuess();
					
					computerOut.writeObject(guess[0]);
					computerOut.writeObject(guess[1]);
					
					boolean hit = (Boolean) computerIn.readObject();
					int ship = (Integer) computerIn.readObject();
					boolean gameOver = (Boolean) computerIn.readObject();
					
					if (gameOver)
					{
						return;
					}
					
					if (hit)
					{
						if (!ontoShip)
						{
							ontoShip = true;
							origin = guess;
							direction = (int) (Math.random() * 4);
							distanceFromOrigin = 1;
							boolean[] temp = {false, false, false, false};
							directionEliminated = temp;
						}
						
						else
						{
							nextOrigins.add(guess);
						}
						
						Ship s = defenseCells[guess[0]][guess[1]].getShip();
						Ship s2 = defenseCells[origin[0]][origin[1]].getShip();
						
						if (s.isSunk())
						{
							if (!s2.isSunk())
							{
								directionEliminated[direction] = true;
								
								if (directionEliminated[(direction + 1) % 4])
								{
									direction = (direction + 1) % 4;
								}
								
								else
								{
									direction -= 1;
									if (direction == - 1)
									{
										direction = 3;
									}
								}
								
								distanceFromOrigin = 1;
							}
							
							else
							{
								if (!nextOrigins.isEmpty())
								{
									for (DefenseCell d: s.getLocations())
									{
										int[] temp = {d.getRow(), d.getCol()};
										for (int i = 0; i < nextOrigins.size(); i++)
										{
											if (Arrays.equals(nextOrigins.get(i), temp))
											{
												nextOrigins.remove(i);
												i--;
											}
										}	
									}
								}
								
								if (!nextOrigins.isEmpty())
								{
									origin = nextOrigins.get(0);
									nextOrigins.remove(0);
									
									ontoShip = true;
									direction = (int) (Math.random() * 4);
									distanceFromOrigin = 1;
									boolean[] temp = {false, false, false, false};
									directionEliminated = temp;
								}
								
								else
								{
									ontoShip = false;
								}
							}
						}
					}
					
					else
					{
						directionEliminated[direction] = true;
						if (distanceFromOrigin > 1)
						{
							direction = (direction + 2) % 4;
							distanceFromOrigin = 1;
						}
					}
				}	
			}
			
			catch (Exception e)
			{
				exitProgram(e);
			}
		}
	}
	
	public static void main (String[] args)
	{
		try
		{
			hitSound = Applet.newAudioClip(Battleships.class.getResource("hitSound.wav"));
			missSound = Applet.newAudioClip(Battleships.class.getResource("missSound.wav"));
			menuBackgroundMusic = Applet.newAudioClip(Battleships.class.getResource("menuBackgroundMusic.wav"));
			gameBackgroundMusic = Applet.newAudioClip(Battleships.class.getResource("gameBackgroundMusic.wav"));
		}
		
		catch (Exception e)
		{
			soundsOn = false;
		}
		
		if (soundsOn)
		{
			menuBackgroundMusic.loop();
		}
		
		showGameTypeDetermination();
	}
}
