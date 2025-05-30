/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.data;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import org.json.JSONObject;

import programmingtheiot.common.ConfigConst;

/**
 * Shell representation of class for student implementation.
 *
 */
public class DataUtil
{
	// static
	private static final Logger _Logger = Logger.getLogger(DataUtil.class.getName());
	private static final DataUtil _Instance = new DataUtil();

	/**
	 * Returns the Singleton instance of this class.
	 * 
	 * @return ConfigUtil
	 */
	public static final DataUtil getInstance()
	{
		return _Instance;
	}
	
	
	// private var's
	
	
	// constructors
	
	/**
	 * Default (private).
	 * 
	 */
	private DataUtil()
	{
		super();
	}
	
	
	// public methods
	
    public String actuatorDataToJson(ActuatorData data)
    {
        String jsonData =null;
        if (data!=null){
            Gson gson = new Gson();
            return gson.toJson(data);
    }
        return jsonData;
    }

	
    public String sensorDataToJson(SensorData data)
    {
        String jsonData =null;
        if (data!=null){
            Gson gson = new Gson();
            return gson.toJson(data);
    }
        return jsonData;
    }
	
    public String systemPerformanceDataToJson(SystemPerformanceData data)
    {
        String jsonData =null;
        if (data!=null){
            Gson gson = new Gson();
            return gson.toJson(data);
    }
        return jsonData;
	}
	
	public String systemStateDataToJson(SystemStateData data)
	{
        String jsonData =null;
        if (data!=null){
            Gson gson = new Gson();
            return gson.toJson(data);
    }
        return jsonData;
	}
	

	public ActuatorData jsonToActuatorData(String jsonData)
	{
        ActuatorData data =null;
        if (jsonData !=null &&jsonData.trim().length() >0) {
            Gson gson =new Gson();
            data =gson.fromJson(jsonData,ActuatorData.class);
                }
        return data;
	}

	
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
        SystemPerformanceData data =null;
        if (jsonData !=null &&jsonData.trim().length() >0) {
            Gson gson =new Gson();
            data =gson.fromJson(jsonData,SystemPerformanceData.class);
                }
        return data;
        }

	public SensorData jsonToSensorData(String jsonData)
	{
        SensorData data =null;
        if (jsonData !=null &&jsonData.trim().length() >0) {
            Gson gson =new Gson();
            data =gson.fromJson(jsonData,SensorData.class);
                }
        return data;
        }
	
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
        SystemStateData data =null;
        if (jsonData !=null &&jsonData.trim().length() >0) {
            Gson gson =new Gson();
            data =gson.fromJson(jsonData,SystemStateData.class);
                }
        return data;
	    }


	public String payloadToCloudPayload(String payload){

		String cloudPayload = null;
		JSONObject oldPayloadjson = new JSONObject(payload);
		JSONObject cloudPayloadJson = new JSONObject();
		cloudPayloadJson.put("value", oldPayloadjson.get("value"));
		cloudPayloadJson.put("timestamp", oldPayloadjson.get("timeStampMillis"));

        // Copy the rest of old payload in "context"
        JSONObject context = new JSONObject();
        Iterator<String> keys = oldPayloadjson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!key.equals("value") && !key.equals("timeStampMillis")) {
                context.put(key, oldPayloadjson.get(key));
            }
        }

        cloudPayloadJson.put("context", context);

		// Convert the cloud payload to a string
		_Logger.info("Cloud payload: " + cloudPayloadJson.toString(2));
		cloudPayload = cloudPayloadJson.toString();

		return cloudPayload;
	}

	public String cloudPayloadToPayload(String payload){
		// Convert the Cloud payload to a format that can be translate to BaseIoTData
		// Parse the cloud-style JSON
		JSONObject cloudPayloadJson = new JSONObject(payload);

		JSONObject originalPayloadJson = new JSONObject();

		originalPayloadJson.put("value", cloudPayloadJson.get("value"));
		originalPayloadJson.put("timeStampMillis", cloudPayloadJson.get("timestamp"));

		if (cloudPayloadJson.has("context")) {
			JSONObject context = cloudPayloadJson.getJSONObject("context");

			Iterator<String> keys = context.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				originalPayloadJson.put(key, context.get(key));
			}
		}

		return originalPayloadJson.toString();
	}

}