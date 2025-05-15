/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Shell representation of class for student implementation.
 *
 */
public class CloudClientConnector implements ICloudClient
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnector.class.getName());
	
	// private var's

	private String topicPrefix = "";
    private MqttClientConnector mqttClient = null;
    private IDataMessageListener dataMsgListener = null;

    // TODO: set to either 0 or 1, depending on which is preferred for your implementation
    private int qosLevel = 1;
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
    public CloudClientConnector() {
        ConfigUtil configUtil = ConfigUtil.getInstance();

        this.topicPrefix = configUtil.getProperty(
            ConfigConst.CLOUD_GATEWAY_SERVICE,
            ConfigConst.BASE_TOPIC_KEY
        );

        // Depending on the cloud service, the topic names may or may not begin with a "/",
        // so this code should be updated according to the cloud service provider's topic naming conventions
        if (topicPrefix == null) {
            topicPrefix = "/";
        } else {
            if (!topicPrefix.endsWith("/")) {
                topicPrefix += "/";
            }
        }
    }
	
	
	// public methods
	
    @Override
    public boolean connectClient() {
        if (this.mqttClient == null) {
            // TODO: either line should work with recent updates to `MqttClientConnector`
            // this.mqttClient = new MqttClientConnector(true);
            this.mqttClient = new MqttClientConnector(ConfigConst.CLOUD_GATEWAY_SERVICE);
        }

        // NOTE: If MqttClientConnector is using the async client, we won't have a complete
        // connection to the cloud-hosted MQTT broker until MqttClientConnector's
        // connectComplete() callback is invoked. The details pertaining to the use
        // of IConnectionListener are covered in PIOT-GDA-11-001 and PIOT-GDA-11-004.
        return this.mqttClient.connectClient();
    }

    @Override
    public boolean disconnectClient()
    {
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            return this.mqttClient.disconnectClient();
        }

        return false;
    }

	@Override
	public void setDataMessageListener(IDataMessageListener listener)
	{
		this.dataMsgListener = listener;
	}

	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data) {
		if (resource != null && data != null) {
			String payload = DataUtil.getInstance().sensorDataToJson(data);
			return publishMessageToCloud(resource, data.getName(), payload);
		}
		return false;
	}

	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data) {
		if (resource != null && data != null) {
			SensorData cpuData = new SensorData();
			cpuData.updateData(data);
			cpuData.setName(ConfigConst.CPU_UTIL_NAME);
			cpuData.setValue(data.getCpuUtilization());

			boolean cpuSuccess = sendEdgeDataToCloud(resource, cpuData);

			if (!cpuSuccess) {
				_Logger.warning("Failed to send CPU utilization data to cloud service.");
			}

			SensorData memData = new SensorData();
			memData.updateData(data);
			memData.setName(ConfigConst.MEM_UTIL_NAME);
			memData.setValue(data.getMemoryUtilization());

			boolean memSuccess = sendEdgeDataToCloud(resource, memData);

			if (!memSuccess) {
				_Logger.warning("Failed to send memory utilization data to cloud service.");
			}

			return cpuSuccess && memSuccess;
		}
		return false;
	}

	@Override
	public boolean subscribeToCloudEvents(ResourceNameEnum resource) {
		boolean success = false;
		String topicName = null;

		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			topicName = createTopicName(resource);
			this.mqttClient.subscribeToTopic(topicName, this.qosLevel);
			success = true;
		} else {
			_Logger.warning("No MQTT connection to broker. Ignoring subscription request. Topic: " + topicName);
		}

		return success;
	}

	@Override
	public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource) {
		boolean success = false;
		String topicName = null;

		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			topicName = createTopicName(resource);
			this.mqttClient.unsubscribeFromTopic(topicName);
			success = true;
		} else {
			_Logger.warning("No MQTT connection to broker. Ignoring unsubscription request. Topic: " + topicName);
		}

		return success;
	}
	
	
	// private methods

	private String createTopicName(ResourceNameEnum resource)
    {
        return createTopicName(resource.getDeviceName(), resource.getResourceType());
    }

    private String createTopicName(String deviceName, String resourceTypeName)
    {
        return this.topicPrefix + deviceName + "/" + resourceTypeName;
    }

    private boolean publishMessageToCloud(ResourceNameEnum resource, String itemName, String payload) {
		String topicName = createTopicName(resource) + "-" + itemName;
		return publishMessageToCloud(topicName, payload);
	}

	private boolean publishMessageToCloud(String topicName, String payload) {
		try {
			_Logger.finest("Publishing payload to CSP: " + topicName);

			this.mqttClient.publishMessage(topicName, payload.getBytes(), this.qosLevel);

			return true;
		} catch (Exception e) {
			_Logger.warning("Failed to publish message to CSP: " + topicName);
		}
		return false;
	}

	
}
