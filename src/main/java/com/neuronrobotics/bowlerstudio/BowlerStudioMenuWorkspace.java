package com.neuronrobotics.bowlerstudio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;

import javafx.application.Platform;
import javafx.scene.control.Menu;

@SuppressWarnings("restriction")
public class BowlerStudioMenuWorkspace {
	private static Menu workspaceMenu;
	private static HashMap<String, Object> workspaceData = null;
	private static final int maxMenueSize = 15;
	private static boolean sorting = false;
	private static HashMap<String,Integer> rank = new HashMap<String, Integer>();
	private static boolean running = false;
	public static void init(Menu workspacemenu) {
		if (workspacemenu == null)
			throw new RuntimeException();
		workspaceMenu = workspacemenu;
	}

	public static void loginEvent() {
		if(running)
			return;
		running = true;
		rank.clear();
		workspaceData = ConfigurationDatabase.getParamMap("workspace");
		new Thread(()-> {
			for (Iterator<String> iterator = workspaceData.keySet().iterator(); iterator.hasNext();) {
				String o = iterator.next();
				try {
					ScriptingEngine.pull(o);
				} catch (Exception e) {
					workspaceData.remove(o);
				}
			}
			running = false;
			
		}).start();
		sort();
	}

	public static void add(String url) {
		add(url, BowlerStudioMenu.gitURLtoMessage(url));
	}

	@SuppressWarnings("unchecked")
	public static void add(String url, String menueMessage) {
		if (menueMessage == null || menueMessage.length() < 2)
			throw new RuntimeException();
		ArrayList<String> data;
		synchronized (workspaceData) {
			if (workspaceData.get(url) == null) {
				data = new ArrayList<String>();
				data.add(menueMessage);
				data.add(new Long(System.currentTimeMillis()).toString());
				workspaceData.put(url, data);
				System.out.println("Workspace add: "+url);
			}
		}
		//data = (ArrayList<String>) workspaceData.get(url);
		//data.set(1, new Long(System.currentTimeMillis()).toString());
		sort();
		//

	}

	@SuppressWarnings("unchecked")
	private static void sort() {
		if (sorting)
			return;
		sorting = true;
		
		boolean rankChanged=false;
		try {
			ArrayList<String> myOptions = new ArrayList<String>();
			synchronized (workspaceData) {
				for (String o : workspaceData.keySet()) {
					//System.out.println("Opt: "+o);
					myOptions.add(o);
				}
			}
			ArrayList<String> menu = new ArrayList<>();
			while (myOptions.size() > 0) {
				int bestIndex = 0;
				String besturl = (String) myOptions.get(bestIndex);
				long newestTime = Long.parseLong(((ArrayList<String>) workspaceData.get(besturl)).get(1));
				for (int i = 0; i < myOptions.size(); i++) {
					String nowurl = (String) myOptions.get(i);
					long myTime = Long.parseLong(((ArrayList<String>) workspaceData.get(nowurl)).get(1));
					if (myTime >= newestTime) {
						newestTime = myTime;
						besturl = nowurl;
						bestIndex = i;
					}
				}
				String removedURL = (String) myOptions.remove(bestIndex);
				if (menu.size() < maxMenueSize) {

					// clone all repos from git
					try {
						// ScriptingEngine.pull(removedURL);
						menu.add(removedURL);
					} catch (Exception e) {
						// repo is broken or missing
						e.printStackTrace();
						System.out.println("Removing from workspace: " + removedURL);
						synchronized (workspaceData) {
							workspaceData.remove(removedURL);
						}
					}

				} else {
					System.out.println("Removing from workspace: " + removedURL);
					synchronized (workspaceData) {
						workspaceData.remove(removedURL);
					}
				}
			}
			
			for(int i=0;i<menu.size();i++) {
				String url = menu.get(i);
				if(rank.get(url)==null) {
					rankChanged=true;
					rank.put(url,i);
					//System.out.println("Rank firstNoted : "+url+" "+i);
				}
				if(rank.get(url).intValue()!=i) {
					rankChanged=true;
					System.out.println("Rank changed : "+url+" to: "+i+" was: "+rank.get(url).intValue());
				}
				rank.put(url,i);
			}
			if(rankChanged) {
				Platform.runLater(() -> {
					if (workspaceMenu.getItems() != null)
						workspaceMenu.getItems().clear();
					
					new Thread(() -> {
						for (String url : menu) {
								BowlerStudioMenu.setUpRepoMenue(workspaceMenu, 
										url, 
										false, 
										false,
										((ArrayList<String>) workspaceData.get(url)).get(0));
							
						}
						sorting = false;
					}).start();
				});
			}else {
				sorting = false;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if(rankChanged) {
			System.out.println("Sorting workspace...");
			new Thread(()->{
				ConfigurationDatabase.save();
			}).start();
		}
	}

}
