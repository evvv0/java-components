/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import programmingtheiot.common.ConfigConst;

/**
 * Hola
 * Convenience wrapper to store system state data, including location
 * information, action command, state data and a list of the following
 * data items:
 * <p>SystemPerformanceData
 * <p>SensorData
 *
 */
public class SystemStateData extends BaseIotData implements Serializable
{
	// static


	// private var's
    private int command = ConfigConst.DEFAULT_COMMAND;
    private List<SystemPerformanceData> sysPerfDataList = null;
    private List<SensorData> sensorDataList = null;

    // constructors
    public SystemStateData()
    {
        super();
        super.setName(ConfigConst.SYS_STATE_DATA);
        super.setStatusCode(ConfigConst.DEFAULT_STATUS);

        this.sysPerfDataList =new ArrayList<>();
        this.sensorDataList  =new ArrayList<>();
        this.command = ConfigConst.DEFAULT_COMMAND;
    }


	// public methods

    public void addSystemPerformanceData(SystemPerformanceData sysPerfData) {
        if (sysPerfData != null) {
            this.sysPerfDataList.add(sysPerfData);
        } else {
            System.out.println("Invalid system performance data");
        }
    }

    // Method to add sensor data
    public void addSensorData(SensorData sensorData) {
        if (sensorData != null) {
            this.sensorDataList.add(sensorData);
        } else {
            System.out.println("Invalid sensor data");
        }
    }

    public List<SystemPerformanceData> getSystemPerformanceDataList() {
        return this.sysPerfDataList;
    }

    public List<SensorData> getSensorDataList() {
        return this.sensorDataList;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public int getCommand() {
        return this.command;
    }



    protected void handleUpdateData(BaseIotData data) {
        if (data instanceof SensorData) {
            SensorData sData = (SensorData) data;
            this.addSensorData(sData);
        } else if (data instanceof SystemPerformanceData) {
            SystemPerformanceData spData = (SystemPerformanceData) data;
            this.addSystemPerformanceData(spData);
        }
    }

    @Override
    public void updateData(BaseIotData data) {
        if (data instanceof SystemStateData) {
            SystemStateData ssd = (SystemStateData) data;

            this.setName(ssd.getName());
            this.command = ssd.getCommand();
            this.setStatusCode(ssd.getStatusCode());

            this.sysPerfDataList.clear();
            this.sysPerfDataList.addAll(ssd.getSystemPerformanceDataList());

            this.sensorDataList.clear();
            this.sensorDataList.addAll(ssd.getSensorDataList());

        }
    }
}