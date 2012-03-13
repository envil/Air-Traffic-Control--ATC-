package Plan;

//********
//* RUNWAY APPROACH SIMULATION - GRAPHICS AND USER INTERACTION
//*
//* a demonstration program for a simplistic simulation of
//* aircraft arrivals to a single runway;
//* simulation runs for one hour;
//* the program uses concept of a simple queuing system with
//* one implicit server (runway) and aircraft objects;
//* the generator object generates the aircraft using random
//* numbers, and determining if there is space for a new airplane
//*
//* when runway is busy the aircraft waits in an airborne queue;
//* an airplane can be released from the airborne queue by clicking on it,
//*
//* originally written by A.J. Kornecki, CSD ERAU, January 1995
//* JAVA port by Robert Temple, http://www.db.erau.edu/~templer/
//* September 1995
//*
//*******************
//* Updated November 1995
//*******************

import java.awt.*;
import java.awt.image.*;
import java.applet.Applet;
import java.util.*;

public class PatternSimulator extends Applet {
	SimCanvas canvas;
	public Label time_label;
	public Label number_label;
	Button pause_button;

	public void init() {	
		canvas = new SimCanvas();
		canvas.init(this);
		setLayout(new BorderLayout());
		Panel p = new Panel();
		p.setFont(new Font("TimesRoman", Font.BOLD, 12));
		p.add(new Label("Simulation Time:", Label.RIGHT));
		p.add(time_label = new Label("1000", Label.LEFT));
		p.add(new Label("Airplanes in system:", Label.RIGHT));
		p.add(number_label = new Label("0", Label.LEFT));
		p.add(new Label("     "));
		p.add(pause_button = new Button("Start Sim"));
		pause_button.disable();
		add("North", p);
		add("Center", canvas);
	}

	public void start() {
		if (canvas.sim_thread == null) {
			canvas.sim_thread = new Thread(canvas);
			canvas.sim_thread.start();
		}
	}

	public void stop() {
		canvas.sim_thread = null;
	}

	public boolean action(Event evt, Object arg) {
		if ("Start Sim".equals(arg)) {
			pause_button.setLabel("Pause");
			canvas.startSim();
			return true;
		}
		else if("Pause".equals(arg)) {
			canvas.paused = true;
			pause_button.setLabel("Resume");
			return true;
		}
		else if("Resume".equals(arg)) {
			canvas.paused = false;
			pause_button.setLabel("Pause");
		}
		return false;
	}
}

class SimCanvas extends Canvas implements Runnable {
	Applet app;

	public Thread sim_thread = null;

	Image background;
	Image hangar;
	Image double_buffer;
	Graphics double_buffer_graphics;
	int mouse_down_at_x;
	int mouse_down_at_y;
	public Vector airplanes;
	int simulation_units;
	final static double new_plane_frequency = 0.10;
	
	public boolean paused = true;

	public void init(Applet app) {
		resize(450, 450);
		setBackground(new Color(10, 70, 30));
		this.app = app;
		background = app.getImage(app.getDocumentBase(), "backgrnd.gif");
		hangar = app.getImage(app.getDocumentBase(), "hangar.gif");
		CommercialAirplane.MakeImages(app.getImage(app.getDocumentBase(), "777.gif"), app);
		PropellerAirplane.MakeImages(app.getImage(app.getDocumentBase(), "propeller.gif"), app);
		double_buffer = app.createImage(450, 450);
		double_buffer_graphics = double_buffer.getGraphics();
		double_buffer_graphics.setColor(new Color(10, 70, 30));
		double_buffer_graphics.fillRect(0,0,450,450);
		double_buffer_graphics.setColor(Color.white);
		double_buffer_graphics.drawString("Loading Pattern Simulator...", 10, 425);
		airplanes = new Vector(20);
	}

	public void startSim() {
		airplanes.removeAllElements();
		airplanes.addElement(new CommercialAirplane());
		repaint();
		simulation_units = 1000;
		paused = false;
	}

	public void paint(Graphics g) {
		double_buffer_graphics.drawImage(background, 0, 0, app);
		for (Enumeration e = airplanes.elements() ; e.hasMoreElements() ;) {
			Airplane plane = (Airplane)e.nextElement();
			plane.Draw(double_buffer_graphics, app);
		}
		double_buffer_graphics.drawImage(hangar, 170, 292, app);
		g.drawImage(double_buffer, 0, 0, app);
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void run() {
		//# Get the first image to the screen ASAP
		while((app.checkImage(background, app) & ImageObserver.ALLBITS) == 0) {
			repaint();
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {}
		}

		((PatternSimulator)getParent()).pause_button.enable();

		while (true) {
			if(paused) {
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {}
			}
			else {
				if(--simulation_units <= 0) {
					((PatternSimulator)getParent()).pause_button.setLabel("Start Sim");
					paused = true;
				}
				((PatternSimulator)getParent()).time_label.setText(Integer.toString(simulation_units));
				((PatternSimulator)getParent()).number_label.setText(Integer.toString(airplanes.size()));
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
				DoUpdate();
				try {
					paint(getGraphics());
				}
				catch (Exception e){
					sim_thread = null;
				}
			}
		}
	}

	protected void DoUpdate() {
		boolean CanAddAirplane = true;
		boolean CanLandAirplane = true;
		for (Enumeration e = airplanes.elements() ; e.hasMoreElements() ;) {
			Airplane plane = (Airplane)e.nextElement();
			if(plane.InTheWayOfNewAirplane()) {
				CanAddAirplane = false;
			}
			if(plane.CurrentlyLanding()) {
				CanLandAirplane = false;
			}
		}
		for (Enumeration e = airplanes.elements() ; e.hasMoreElements() ;) {
			Airplane plane = (Airplane)e.nextElement();
			if(CanLandAirplane) {
				if(plane.DoLandIfPossible()) {
					CanLandAirplane = false;
				}
			}
			if(mouse_down_at_x >= 0) {
				plane.Release(mouse_down_at_x, mouse_down_at_y);
			}
			plane.Fly();
			if(plane.RemoveMe()) {
				airplanes.removeElement(plane);
			}
		}
		if(CanAddAirplane) {
			double random_number = Math.random();
			if(random_number < new_plane_frequency / 2) {
				airplanes.addElement(new PropellerAirplane());
			}
			else if(random_number < new_plane_frequency) {
				airplanes.addElement(new CommercialAirplane());
			}
		}
	}

	public boolean mouseDown(java.awt.Event evt, int x, int y) {
		mouse_down_at_x = x;
		mouse_down_at_y = y;
		return true;
	}

	public boolean mouseUp(java.awt.Event evt, int x, int y) {
		mouse_down_at_x = -1;
		mouse_down_at_y = -1;
		return true;
	}

	public boolean mouseDrag(java.awt.Event evt, int x, int y) {
		mouse_down_at_x = x;
		mouse_down_at_y = y;
		return true;
	}
}

abstract class Airplane extends Object {
	public static final int NORTH 		= 0;
	public static final int NORTHEAST = 1;
	public static final int EAST 			= 2;
	public static final int SOUTHEAST = 3;
	public static final int SOUTH 		= 4;
	public static final int SOUTHWEST = 5;
	public static final int WEST 			= 6;
	public static final int NORTHWEST = 7;

	public static final int SPEED = 18;
	public static final int DIAGNAL_SPEED = 10;
	public static final int GROUND_SPEED = 5;
	public static final int DIAGNAL_GROUND_SPEED = 3;

	public static final int NORMAL = 0;
	public static final int LANDING = 1;
	public static final int LEAVING = 2;

	int x, y, w, h;
	int direction;
	int status;
	int random_course;	

	Airplane() {
		this.x = 410;
		this.y = 0;
		direction = SOUTH;
		status = NORMAL;
		random_course  = (int) (Math.random() * 20.0f);
	}

	public abstract void Draw(Graphics g, ImageObserver ob);
	
	public boolean InTheWayOfNewAirplane() {
		if(status == LANDING)
			return false;
		switch(direction) {
			case EAST :
				return (x > 265);
			case SOUTHEAST :
				return status != LANDING;
			case SOUTH :
				return (status != LANDING && y < 135);
			default :
				return false;
		}
	}

	public boolean CurrentlyLanding() {
		return (status == LANDING && direction <= SOUTH);
	}

	public boolean RemoveMe() {
		if(status == LANDING && direction == NORTH)
			return true;
		if(status == LEAVING &&(x < -40 || y < -40 || x > 450 || y > 450))
			return true;
		return false;
	}

	public boolean DoLandIfPossible() {
		if(status == NORMAL && direction == EAST && x >= 250 && x <= 275) {
			status = LANDING;
			direction = SOUTHEAST;
			return true;
		}
		return false;
	}

	public void Fly() {
		if(status == LANDING) {
			switch(direction) {
				case SOUTHEAST :
					y += DIAGNAL_SPEED;
					x += DIAGNAL_SPEED;
					if(x >= 280) {
						x = 282;
						direction = SOUTH;
					}
					break;
				case SOUTH :
					int gradual_slow = y / 20;
					y += SPEED - gradual_slow;
					if(y >= 269) {
						direction = SOUTHWEST;
					}
					break;
				case SOUTHWEST :
					x -= DIAGNAL_GROUND_SPEED;
					y += DIAGNAL_GROUND_SPEED;
					if(y >= 292) {
						direction = WEST;
					}
					break;
				case WEST :
					x -= GROUND_SPEED;
					if(x <= 220) {
						direction = NORTH;
					}
					break;
			}
		}
		else {
			switch(direction) {
				case NORTH :
					y -= SPEED;
					if(y + random_course < 65 && status != LEAVING) {
						direction = NORTHEAST;
						random_course  = (int) (Math.random() * 20.0f);
					}
					break;
				case NORTHEAST :
					y -= DIAGNAL_SPEED;
					x += DIAGNAL_SPEED;
					if(y + random_course < 35 && status != LEAVING) {
						direction = EAST;
					}
					break;
				case EAST :
					x += SPEED;
					if(x + random_course > 370 && status != LEAVING) {
						direction = SOUTHEAST;
						random_course  = (int) (Math.random() * 20.0f);
					}
					break;
				case SOUTHEAST :
					x += DIAGNAL_SPEED;
					y += DIAGNAL_SPEED;
					if(x + random_course > 400 && status != LEAVING) {
						direction = SOUTH;
					}
					break;
				case SOUTH :
					y += SPEED;
					if(y + random_course > 358 && status != LEAVING) {
						direction = SOUTHWEST;
						random_course  = (int) (Math.random() * 20.0f);
					}
					break;
				case SOUTHWEST :
					x -= DIAGNAL_SPEED;
					y += DIAGNAL_SPEED;
					if(y + random_course > 388 && status != LEAVING) {
						direction = WEST;
					}
					break;
				case WEST :
					x -= SPEED;
					if(x + random_course < 62 && status != LEAVING) {
						direction = NORTHWEST;
						random_course  = (int) (Math.random() * 20.0f);
					}
					break;
				case NORTHWEST :
					x -= DIAGNAL_SPEED;
					y -= DIAGNAL_SPEED;
					if(x + random_course < 32 && status != LEAVING) {
						direction = NORTH;
					}
					break;
			}
		}
	}

	public void Release(int x_press, int y_press) {

		if(				status == NORMAL && 
							x_press >= x && x_press <= x + w && 
							y_press >= y && y_press <= y + h) {

			status = LEAVING;

		}
	}
}

class CommercialAirplane extends Airplane {
	public static Image airplane_image[] = new Image[8];

	public static void MakeImages(Image base_image, Component component) {
		for(int y = 0 ; y < 8 ; ++y) {
			
			ImageFilter crop = new CropImageFilter(0, 38 * y, 36, 38);

			airplane_image[y] = component.createImage(
								new FilteredImageSource(base_image.getSource(), crop));

			component.prepareImage(airplane_image[y], component);

		}
	}

	public CommercialAirplane() {
		super();
		w = 36;
		h = 38;
	}

	public void Draw(Graphics g, ImageObserver ob) {
		g.drawImage(airplane_image[direction], x, y, ob);
	}
}

class PropellerAirplane extends Airplane {
	public static Image airplane_image[] = new Image[8];

	public static void MakeImages(Image base_image, Component component) {		
		for(int y = 0 ; y < 8 ; ++y) {
			ImageFilter crop = new CropImageFilter(0, 31 * y, 31, 31);

			airplane_image[y] = component.createImage(
								new FilteredImageSource(base_image.getSource(), crop));

			component.prepareImage(airplane_image[y], component);

		}
	}

	public PropellerAirplane() {
		super();
		w = 31;
		h = 31;
	}

	public void Draw(Graphics g, ImageObserver ob) {
		g.drawImage(airplane_image[direction], x, y, ob);
	}
}

