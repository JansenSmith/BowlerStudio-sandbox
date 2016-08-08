package com.neuronrobotics.bowlerstudio.creature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.BowlerStudioController;
import com.neuronrobotics.bowlerstudio.assets.AssetFactory;
import com.neuronrobotics.bowlerstudio.physics.MobileBasePhysicsManager;
import com.neuronrobotics.bowlerstudio.physics.PhysicsEngine;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;
import com.neuronrobotics.bowlerstudio.threed.MobileBaseCadManager;
import com.neuronrobotics.sdk.addons.kinematics.DHLink;
import com.neuronrobotics.sdk.addons.kinematics.LinkConfiguration;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import com.neuronrobotics.sdk.addons.kinematics.imu.IMUUpdate;
import com.neuronrobotics.sdk.addons.kinematics.imu.IMUUpdateListener;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.IDeviceConnectionEventListener;
import com.neuronrobotics.sdk.util.ThreadUtil;

import eu.mihosoft.vrl.v3d.CSG;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class CreaturePhysicsWidget extends GridPane  implements IMUUpdateListener {
	Button runstop = new Button("Run", AssetFactory.loadIcon("Run.png"));
	Button pauseresume = new Button("Pause", AssetFactory.loadIcon("Pause.png"));
	Button step = new Button("Step", AssetFactory.loadIcon("Step.png"));
	TextField msLoopTime =new TextField("200") ;
	int msLoopTimeInt =0;
	private boolean run=false;
	private boolean takestep=false;
	private boolean pause=false;
	Thread physicsThread =null;
	private Set<CSG> oldParts=null;
	private MobileBase base;

	public CreaturePhysicsWidget(MobileBase base){

		this.base = base;
		base.addConnectionEventListener(new IDeviceConnectionEventListener() {
			
			@Override
			public void onDisconnect(BowlerAbstractDevice arg0) {
				stop();
			}
			
			@Override
			public void onConnect(BowlerAbstractDevice arg0) {}
		});
		add(runstop,0,0);
		add(pauseresume,1,0);
		add(step,2,0);
		add(new Label("MS loop"),3,0);
		add(msLoopTime,4,0);
		pauseresume.setDisable(true);
		step.setDisable(true);
		pauseresume.setOnAction(event->{
			if(!isPause()){
				pauseresume.setGraphic(AssetFactory.loadIcon("Resume.png"));
				pauseresume.setText("Resume");
				step.setDisable(false);
			}else{
				pauseresume.setGraphic(AssetFactory.loadIcon("Pause.png"));
				pauseresume.setText("Pause");
				step.setDisable(true);
			}
			setPause(!isPause());
		});
		step.setOnAction(event->{
			setTakestep(true);
		});
		runstop.setOnAction(event->{
			if(isRun()){
				stop();
				new Thread(){
					public void run(){
						ThreadUtil.wait(50);
						System.gc();// clean up any objects created by the physics engine
					}
				}.start();
			}else{
				runstop.setGraphic(AssetFactory.loadIcon("Stop.png"));
				runstop.setText("Stop");
				msLoopTime.setDisable(true);
				pauseresume.setDisable(false);
				base.getImu().addvirtualListeners(this);
				new Thread(){
					

					public void run(){
						while(MobileBaseCadManager.get( base).getProcesIndictor().getProgress()<1){
							ThreadUtil.wait(100);
						}
						HashMap<LinkConfiguration, ArrayList<CSG>> simplecad = MobileBaseCadManager.getSimplecad(base) ;
						ArrayList<CSG> baseCad=MobileBaseCadManager.getBaseCad(base);
						base.DriveArc(new TransformNR(), 0);
						PhysicsEngine.get().clear();
						new MobileBasePhysicsManager(base, baseCad, simplecad);
						BowlerStudio3dEngine threeD = BowlerStudioController.getBowlerStudio().getJfx3dmanager();
						oldParts = threeD.getCsgMap().keySet();
						BowlerStudioController.setCsg(PhysicsEngine.get().getCsgFromEngine());
						int loopTiming = (int) Double.parseDouble(msLoopTime.getText());
						
						physicsThread = new Thread(){
							public void run(){
								try{
									while(!Thread.interrupted() && isRun()){
										while(!Thread.interrupted() && isPause() && isTakestep()==false){
											ThreadUtil.wait(loopTiming);
										}
										setTakestep(false);
										long start = System.currentTimeMillis();
										PhysicsEngine.get().stepMs(loopTiming);
										long took = (System.currentTimeMillis() - start);
										if (took < loopTiming)
											ThreadUtil.wait((int) (loopTiming - took)/4);
										else{
											if((loopTiming - took)>loopTiming*2)
											System.out.println("ERROR Real time broken by: "+(loopTiming - took)+"ms");
										}
									}
								}catch(Exception e){
									
								}
							}
						};
						physicsThread.start();
					}
				}.start();
				
			}
			setRun(!isRun());
		});
		
	}
	private void stop() {
		runstop.setGraphic(AssetFactory.loadIcon("Run.png"));
		runstop.setText("Run");
		if(physicsThread!=null)
			physicsThread.interrupt();
		
		msLoopTime.setDisable(false);
		pauseresume.setDisable(true);
		if(oldParts!=null){
			ArrayList<CSG>oldp=new ArrayList<>();
			for(CSG c:oldParts){
				oldp.add(c);
			}
			BowlerStudioController.setCsg(oldp);
			oldParts=null;
		}
		base.getImu().removevirtualListeners(this);
	}
	public boolean isTakestep() {
		return takestep;
	}
	public void setTakestep(boolean takestep) {
		this.takestep = takestep;
	}
	public boolean isPause() {
		return pause;
	}
	public void setPause(boolean pause) {
		this.pause = pause;
	}
	public boolean isRun() {
		return run;
	}
	public void setRun(boolean run) {
		this.run = run;
	}
	@Override
	public void onIMUUpdate(IMUUpdate arg0) {
//		System.err.println("X = "+arg0.getxAcceleration()+
//				" Y = "+arg0.getyAcceleration()+
//				" Z = "+arg0.getzAcceleration()+
//				" rX = "+arg0.getRotxAcceleration()+
//				" rY = "+arg0.getRotyAcceleration()+
//				" rZ = "+arg0.getRotzAcceleration()
//		
//				);
	}
}
