package org.processmining.plugins.unfolding;

import java.awt.Color;

public class Palette{

	
	Color cutColor;
	Color deadColor;
	Color bothCutoffDead;
	
	public Palette(){
		cutColor = new Color(0,255,255); //Cyan
		deadColor = new Color(255,77,77); // light red
		bothCutoffDead = new Color(138,43,226); //violet
		
	}
	
	public Color getBothCutoffDead() {
		return bothCutoffDead;
	}

	public Color getDeadColor() {
		return deadColor;
	}


	public Color getCutColor(){
		return cutColor;
	}
	
	
}
