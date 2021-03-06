package com.creatingskies.game.core;

import k8055.K8055JavaCall;

import com.creatingskies.game.classes.AbstractInputReader;
import com.creatingskies.game.model.Constant;

public class K8055AnalogInputReader extends AbstractInputReader {

	private InputForce inputForce;
	private K8055JavaCall k8055;
	
	private long interval = 50; 
	private long duration = 0;
	
	private int previousValue = 0;
	
	private int previousVerticalTiltValue = 0;
	private int previousHorizontalTiltValue = 0;
	
	private Integer verticalChannels[] = {3, 4, 5};
	private Integer horizontalChannels[] = {6, 7, 8};
	
	@Override
	public void init() {
		inputForce = new InputForce();
		k8055 = new K8055JavaCall();
		k8055.OpenDevice(0);
	}

	@Override
	public void destroy() {
		clearAllDigital();
		k8055.CloseDevice();
	}

	public void clearAllDigital() {
		k8055.ClearAllDigital();
	}

	@Override
	public InputForce readInput() {
		inputForce.right = (int) ((k8055.ReadAnalogChannel(1) * Constant.MAX_MOVESPEED) / 255) * 2;
		inputForce.left = (int) ((k8055.ReadAnalogChannel(2) * Constant.MAX_MOVESPEED) / 255) * 2;
		
		if(k8055.ReadDigitalChannel(1) == 1) {
			setResetButtonPressed(true);
		}else{
			setResetButtonPressed(false);
		}
    	if(k8055.ReadDigitalChannel(2) == 1) {
    		setQuitButtonPressed(true);
    	}else{
    		setQuitButtonPressed(false);
    	}
		
		return inputForce;
	}

	@Override
	public void display(Double speed, Double slowFactor, Double degree,
			Integer verticalTilt, Integer horizontalTilt) {
		duration += interval;
		
		if(duration >= 500){
			duration = 0;
			
			boolean shouldUpdateDisplay = false;
			
			if(previousValue < slowFactor.intValue()){
				shouldUpdateDisplay = true;
				previousValue++;
			} else if(previousValue > slowFactor.intValue()){
				shouldUpdateDisplay = true;
				previousValue--;
			}
			
			if(previousVerticalTiltValue < verticalTilt){
				shouldUpdateDisplay = true;
				previousVerticalTiltValue++;
			} else if(previousVerticalTiltValue > verticalTilt){
				shouldUpdateDisplay = true;
				previousVerticalTiltValue--;
			}
			
			if(previousHorizontalTiltValue < horizontalTilt){
				shouldUpdateDisplay = true;
				previousHorizontalTiltValue++;
			} else if(previousHorizontalTiltValue > horizontalTilt){
				shouldUpdateDisplay = true;
				previousHorizontalTiltValue--;
			}
			
			if(shouldUpdateDisplay){
				clearAllDigital();
				displayDifficulty(previousValue);
				displayTilt(previousVerticalTiltValue, true);
				displayTilt(previousHorizontalTiltValue, false);
			}
		}
	}
	
	public void displayDifficulty(int data){
		if(data >= 4){
//			maximum difficulty display is 3 due to tilting
			data = 3;
		}
		
		if(data >= 2){
			k8055.SetDigitalChannel(2);
			data -= 2;
		}
		
		if(data >= 1){
			k8055.SetDigitalChannel(1);
			data -= 1;
		}
	}
	
	public void displayTilt(int data, boolean forVertical){
		Integer[] channels = forVertical ? verticalChannels : horizontalChannels;
		
//		maximum tilt is 3 due to limited output channel
		if(data >= 4){
			data = 3;
		}
		
//		minimum tilt is -3 due to limited output channel
		if(data <= -4){
			data = -3;
		}
		
		if(data > 0){
			k8055.SetDigitalChannel(channels[0]);
		}
		
		data = Math.abs(data);
		
		if(data >= 2){
			k8055.SetDigitalChannel(channels[2]);
			data -= 2;
		}
		
		if(data >= 1){
			k8055.SetDigitalChannel(channels[1]);
			data -= 1;
		}
	}

}
