package ca.sqlpower.architect.layout;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;

import ca.sqlpower.architect.swingui.Relationship;
import ca.sqlpower.architect.swingui.TablePane;

public abstract class AbstractLayout implements ArchitectLayoutInterface {

	protected Rectangle frame;
	
	public void setup(List<TablePane> nodes, List<Relationship> edges, Rectangle rect) {
		frame = rect;
		
	}
	
	HashMap<String,Object> properties;
	
	protected AbstractLayout()
	{
		properties = new HashMap<String,Object>();
	}
	
	public void setProperty(String key, Object value) {
		properties.put(key,value);

	}

	public Object getProperty(String key) {
		return properties.get(key);
	}
	
	public Dimension getNewArea(List<TablePane> nodes) {
		Dimension d = new Dimension();
		long width=0;
		long height=0;
        long area=0;
		for (TablePane tp : nodes) {
			Rectangle b = tp.getBounds();
			width += b.width;
			height += b.height;
            area += b.width *b.height;
		}
       
        if (width == 0 || height == 0) return new Dimension();
        
		long avgWidth = width/nodes.size()+1;
        long avgHeight = height/nodes.size() +1;
        long radius = Math.max(avgWidth,avgHeight);
        final double areaFudgeFactor = 16.0;
        double newWidth = Math.sqrt( (11.0/8.5) * area * areaFudgeFactor );
        double newHeight = (8.5/11.0) * newWidth;
		d.setSize((int) newWidth, (int) newHeight);
		return d;
	}

	
	
	
}
