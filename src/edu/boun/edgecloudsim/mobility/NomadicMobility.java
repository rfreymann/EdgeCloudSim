/*
 * Title:        EdgeCloudSim - Nomadic Mobility model implementation
 * 
 * Description: 
 * MobilityModel implements basic nomadic mobility model where the
 * place of the devices are changed from time to time instead of a
 * continuous location update.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.mobility;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.cloudbus.cloudsim.core.CloudSim;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class NomadicMobility extends MobilityModel {
	private Location[] deviceLocations;
	private int[] datacenterDeviceCount;
	ExponentialDistribution[] expRngList;
	
	public NomadicMobility(int _numberOfMobileDevices, double _simulationTime) {
		super(_numberOfMobileDevices, _simulationTime);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void initialize() {
		deviceLocations = new Location[numberOfMobileDevices];
		datacenterDeviceCount = new int[SimSettings.getInstance().getNumOfEdgeDatacenters()];
		for (int i=0; i<datacenterDeviceCount.length;i++){
			datacenterDeviceCount[i] = 0;
		}
		
		expRngList = new ExponentialDistribution[SimSettings.getInstance().getNumOfEdgeDatacenters()];

		//create random number generator for each place
		Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
			String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
			int placeTypeIndex = Integer.parseInt(attractiveness);
			
			expRngList[i] = new ExponentialDistribution(SimSettings.getInstance().getMobilityLookUpTable()[placeTypeIndex]);
		}
		
		//initialize locations of each device and start scheduling of movement events
		for(int i=0; i<numberOfMobileDevices; i++) {
			
			int randDatacenterId = SimUtils.getRandomNumber(0, SimSettings.getInstance().getNumOfEdgeDatacenters()-1);
			Node datacenterNode = datacenterList.item(randDatacenterId);
			Element datacenterElement = (Element) datacenterNode;
			Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
			String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
			int placeTypeIndex = Integer.parseInt(attractiveness);
			int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());

			++datacenterDeviceCount[i];
			deviceLocations[i] = new Location(placeTypeIndex, wlan_id);
			double waitingTime = expRngList[deviceLocations[i].getServingWlanId()].sample();
			SimManager.getInstance().schedule(i,waitingTime+ CloudSim.clock(),SimManager.getMoveDevice());

		}
		


	}


	public void move(int deviceId){

		boolean placeFound = false;
		int currentLocationId = deviceLocations[deviceId].getServingWlanId();
		double waitingTime = expRngList[currentLocationId].sample();
		Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
		NodeList datacenterList = doc.getElementsByTagName("datacenter");

		while(placeFound == false){
			int newDatacenterId = SimUtils.getRandomNumber(0,SimSettings.getInstance().getNumOfEdgeDatacenters()-1);
			if(newDatacenterId != currentLocationId){
				placeFound = true;
				Node datacenterNode = datacenterList.item(newDatacenterId);
				Element datacenterElement = (Element) datacenterNode;
				Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
				String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
				int placeTypeIndex = Integer.parseInt(attractiveness);
				int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());

				--datacenterDeviceCount[currentLocationId];
				++datacenterDeviceCount[wlan_id];
				deviceLocations[deviceId] = new Location(placeTypeIndex, wlan_id);
				SimManager.getInstance().schedule(deviceId,waitingTime+ CloudSim.clock(),SimManager.getMoveDevice());
			}
		}
		if(!placeFound){
			SimLogger.printLine("impossible is occured! location cannot be assigned to the device!");
			System.exit(0);
		}
	}

	@Override
	public Location getLocation(int deviceId) {
		return deviceLocations[deviceId];
	}

	@Override
	public int getDeviceCount(int datacenterId) {
		return datacenterDeviceCount[datacenterId];
	}
}
