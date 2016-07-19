package org.processmining.plugins.unfolding;

import java.awt.Color;

public class Palette{

	
	private Color cutColor;
	private Color deadColor;
	private Color bothCutoffDead;
	private Color LocalConfigurationColor;
	private Color arcLabelColor; 
	

	public Palette(){
		cutColor = new Color(0,255,255); //Cyan
		deadColor = new Color(255,77,77); // light red
		bothCutoffDead = new Color(138,43,226); //violet
		LocalConfigurationColor = Color.GREEN;
		arcLabelColor = Color.RED; 
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
	
	public Color getLocalConfigurationColor(){
		return LocalConfigurationColor;
	}
	
	public Color getArcLabelColor() {
		return arcLabelColor;
	}
}
