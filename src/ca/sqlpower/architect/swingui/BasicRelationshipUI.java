package ca.sqlpower.architect.swingui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import org.apache.log4j.Logger;

/**
 * The BasicRelationshipUI is responsible for drawing the lines
 * between tables.  Subclasses decorate the ends of the lines.
 */
public class BasicRelationshipUI extends RelationshipUI
	implements PropertyChangeListener, java.io.Serializable {

	private static Logger logger = Logger.getLogger(BasicRelationshipUI.class);

	protected Relationship relationship;

	public static final int NO_FACING_EDGES = 0;
	public static final int LEFT_FACES_RIGHT = 1;
	public static final int RIGHT_FACES_LEFT = 2;
	public static final int TOP_FACES_BOTTOM = 4;
	public static final int BOTTOM_FACES_TOP = 8;

	public static ComponentUI createUI(JComponent c) {
		logger.debug("Creating new BasicRelationshipUI for "+c);
        return new BasicRelationshipUI();
    }

    public void installUI(JComponent c) {
		logger.debug("Installing BasicRelationshipUI on "+c);
		relationship = (Relationship) c;
		relationship.addPropertyChangeListener(this);
    }

    public void uninstallUI(JComponent c) {
		relationship = (Relationship) c;
		relationship.removePropertyChangeListener(this);
    }
	
	/**
	 * @param g The graphics to paint on.  It should be in the
	 * coordinate space of the containing playpen.
	 */
    public void paint(Graphics g, JComponent c) {
		logger.debug("BasicRelationshipUI is painting");
		Relationship r = (Relationship) c;
		Graphics2D g2 = (Graphics2D) g;

		Point pktloc = r.getPkConnectionPoint();
		Point start = new Point(pktloc.x + r.getPkTable().getLocation().x,
								pktloc.y + r.getPkTable().getLocation().y);
 		Point fktloc = r.getFkConnectionPoint();
		Point end = new Point(fktloc.x + r.getFkTable().getLocation().x,
							  fktloc.y + r.getFkTable().getLocation().y);

		int orientation = getFacingEdges(relationship.getPkTable(), relationship.getFkTable());
		switch (orientation) {
		case LEFT_FACES_RIGHT:
		case RIGHT_FACES_LEFT:
			int midx = (Math.abs(end.x - start.x) / 2) + Math.min(start.x, end.x);
			g2.drawLine(start.x, start.y, midx, start.y);
			g2.drawLine(midx, start.y, midx, end.y);
			g2.drawLine(midx, end.y, end.x, end.y);
			break;

		case TOP_FACES_BOTTOM:
		case BOTTOM_FACES_TOP:
			int midy = (Math.abs(end.y - start.y) / 2) + Math.min(start.y, end.y);
			g2.drawLine(start.x, start.y, start.x, midy);
			g2.drawLine(start.x, midy, end.x, midy);
			g2.drawLine(end.x, midy, end.x, end.y);
			break;
			
		case NO_FACING_EDGES:
			g2.drawLine(start.x, start.y, end.x, end.y);
			break;
		}

		logger.debug("Drew line from "+start+" to "+end);

		paintTerminations(g2, start, end, orientation);
	}
	
	/**
	 * Paints red dots.  Subclasses will implement real notation.
	 */
	protected void paintTerminations(Graphics2D g2, Point start, Point end, int orientation) {
		Color oldColor = g2.getColor();
		g2.setColor(Color.red);
		g2.fillOval(start.x - 2, start.y - 2, 4, 4);
		g2.fillOval(end.x - 2, end.y - 2, 4, 4);
		g2.setColor(oldColor);
	}

	public void bestConnectionPoints(TablePane tp1, TablePane tp2,
									 Point tp1point, Point tp2point) {
		Rectangle tp1b = tp1.getBounds();
		Rectangle tp2b = tp2.getBounds();
		switch(getFacingEdges(tp1, tp2)) {
		case LEFT_FACES_RIGHT:
			tp1point.move(0, tp1b.height/2);
			tp2point.move(tp2b.width, tp2b.height/2);
			break;

		case RIGHT_FACES_LEFT:
			tp1point.move(tp1b.width, tp1b.height/2);
			tp2point.move(0, tp2b.height/2);
			break;

		case TOP_FACES_BOTTOM:
			tp1point.move(tp1b.width/2, 0);
			tp2point.move(tp2b.width/2, tp2b.height);
			break;

		case BOTTOM_FACES_TOP:
			tp1point.move(tp1b.width/2, tp1b.height);
			tp2point.move(tp2b.width/2, 0);
			break;

		default:
			throw new RuntimeException("Unrecognised answer from getFacingEdges");
		}
	}

	public Point closestEdgePoint(TablePane tp, Point p) {
		Dimension tpsize = tp.getSize();

		// clip point p to inside of tp
		Point bcp = new Point(Math.max(0, Math.min(tpsize.width, p.x)),
							  Math.max(0, Math.min(tpsize.height, p.y)));
		
		boolean adjustX = bcp.y != 0 && bcp.y != tpsize.height;
		boolean adjustY = bcp.x != 0 && bcp.x != tpsize.width;

		// push x-coordinate to left or right edge of tp, if y-coord is inside tp
		if (adjustX) {
			if (bcp.x < (tpsize.width/2)) {
				bcp.x = 0;
			} else {
				bcp.x = tpsize.width;
			}
		}
		
		// push y-coordinate to top or bottom edge of tp, if x-coord is inside tp
		if (adjustY) {
			if (bcp.y < (tpsize.height/2)) {
				bcp.y = 0;
			} else {
				bcp.y = tpsize.height;
			}
		}

		return bcp;
	}

	protected int getFacingEdges(TablePane parent, TablePane child) {
		Rectangle parentb = parent.getBounds();
		Rectangle childb = child.getBounds();
		int pl = getParentTerminationLength();
		int cl = getChildTerminationLength();

		if (parentb.x-pl >= childb.x+childb.width+cl) {
			return LEFT_FACES_RIGHT;
		} else if (parentb.x+parentb.width+pl <= childb.x-cl) {
			return RIGHT_FACES_LEFT;
		} else if (parentb.y-pl >= childb.y+childb.height+cl) {
			return TOP_FACES_BOTTOM;
		} else if (parentb.y+parentb.height+pl <= childb.y-cl) {
			return BOTTOM_FACES_TOP;
		} else {
			return NO_FACING_EDGES;
		}
	}

	public int getParentTerminationLength() {
		return 5;
	}

	public int getChildTerminationLength() {
		return 5;
	}
	// --------------- PropertyChangeListener ----------------------
	public void propertyChange(PropertyChangeEvent e) {
		logger.debug("BasicRelationshipUI notices change of "+e.getPropertyName()
					 +" from "+e.getOldValue()+" to "+e.getNewValue()+" on "+e.getSource());
	}
}
