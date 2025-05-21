/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.SystemStateData;
import programmingtheiot.gda.system.SystemPerformanceManager;
import programmingtheiot.data.BaseIotData;

import programmingtheiot.gda.connection.ICloudClient;
import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;





/**
 * Shell representation of class for student implementation.
 *
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());
	
	// private var's
	
	private boolean enableMqttClient = true;
	private boolean enableCoapServer = true;
	private boolean enableCloudClient = true;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	private boolean enableSystemPerf = false;
	
	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	//private IPubSubClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private SystemPerformanceManager sysPerfMgr = null;
	private ICloudClient cloudClient = null;


	    // Variables de instancia para la gestión de la humedad y los umbrales
    private ActuatorData latestHumidifierActuatorData = null;
    private ActuatorData latestHumidifierActuatorResponse = null;
    private SensorData latestHumiditySensorData = null;
    private OffsetDateTime latestHumiditySensorTimeStamp = null;

    private boolean handleHumidityChangeOnDevice = false; // opcional
    private int lastKnownHumidifierCommand = ConfigConst.OFF_COMMAND;

    // Cargar estos valores desde PiotConfig.props
    private long humidityMaxTimePastThreshold = 300; // segundos
    private float nominalHumiditySetting = 40.0f;
    private float triggerHumidifierFloor = 30.0f;
    private float triggerHumidifierCeiling = 50.0f;


	
	// constructors
	
	public DeviceDataManager()
	{
		super();
		
		ConfigUtil configUtil = ConfigUtil.getInstance();

	    this.enableMqttClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);

	    this.enableCoapServer = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);

	    this.enableCloudClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);

	    this.enablePersistenceClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);

        // Cargar las configuraciones desde el archivo PiotConfig.props
        this.handleHumidityChangeOnDevice = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, "handleHumidityChangeOnDevice");
        this.humidityMaxTimePastThreshold = configUtil.getInteger(ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");
        this.nominalHumiditySetting = configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
        this.triggerHumidifierFloor = configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
        this.triggerHumidifierCeiling = configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");

        // Validación básica para el tiempo de umbral
        if (this.humidityMaxTimePastThreshold < 10 || this.humidityMaxTimePastThreshold > 7200) {
            this.humidityMaxTimePastThreshold = 300; // Valor por defecto
        }


	    initManager();
}

    private void initManager()
    {
        ConfigUtil configUtil = ConfigUtil.getInstance();
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);

        if (this.enableSystemPerf) {
            this.sysPerfMgr = new SystemPerformanceManager();
            this.sysPerfMgr.setDataMessageListener(this);
        }

        if (this.enableMqttClient) {
            this.mqttClient = new MqttClientConnector();
            this.mqttClient.setDataMessageListener(this);
        }

        if (this.enableCoapServer) {
            this.coapServer = new CoapServerGateway(this);

        }

        if (this.enableCloudClient) {
            this.cloudClient = new CloudClientConnector();

        }

        if (this.enablePersistenceClient) {

        }
    }

    @Override
    public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
    {
        if (data != null) {
            _Logger.info("Handling actuator response: " + data.getName());

		    this.handleIncomingDataAnalysis(resourceName, data);

            if (data.hasError()) {
                _Logger.warning("Error flag set for ActuatorData instance.");
            }
            return true;
        } else{
            return false;
    }}


    @Override
    public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
        {
            if (data != null) {
                _Logger.log(
                    Level.FINE,
                    "Actuator request received: 0. Message: 1",
                    new Object[] { resourceName.getResourceName(), data.getCommand() });

                if (data.hasError()) {
                    _Logger.warning("Error flag set for ActuatorData instance.");
                }

                int qos = ConfigConst.DEFAULT_QOS;

                // Aquí puedes implementar lógica adicional de análisis si se requiere

                this.sendActuatorCommandtoCda(resourceName, data);

                return true;
            }
            return false;
        }

    @Override
    public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
    {
        if (resourceName != null && msg != null) {
            try {
                if (resourceName == ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE) {
                    _Logger.info("Handling incoming ActuatorData message: " + msg);

                    // NOTE: it may seem wasteful to convert to ActuatorData and back while
                    // the JSON data is already available; however, this provides a validation
                    // scheme to ensure the data is actually an 'ActuatorData' instance
                    // prior to sending off to the CDA
                    ActuatorData ad = DataUtil.getInstance().jsonToActuatorData(msg);
                    String jsonData = DataUtil.getInstance().actuatorDataToJson(ad);

                    if (this.mqttClient != null) {
                        // TODO: retrieve the QoS level from the configuration file
                        _Logger.fine("Publishing data to MQTT broker: " + jsonData);
                        return this.mqttClient.publishMessage(resourceName, jsonData, 0);
                    }

                    // TODO: If the GDA is hosting a CoAP server (or a CoAP client that
                    // will connect to the CDA's CoAP server), you can add that logic here
                    // in place of the MQTT client or in addition

                } else {
                    _Logger.warning("Failed to parse incoming message. Unknown type: " + msg);

                    return false;
                }
            } catch (Exception e) {
                _Logger.log(Level.WARNING, "Failed to process incoming message for resource: " + resourceName, e);
            }
        } else {
            _Logger.warning("Incoming message has no data. Ignoring for resource: " + resourceName);
        }

        return false;
    }

    @Override
    public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data) {
        if (data != null) {
            _Logger.fine("Handling sensor message: " + data.getName());

            if (data.hasError()) {
                _Logger.warning("Error flag set for SensorData instance.");
            }

            String jsonData = DataUtil.getInstance().sensorDataToJson(data);
            _Logger.fine("JSON [SensorData] -> " + jsonData);

            int qos = ConfigConst.DEFAULT_QOS;

            if (this.enablePersistenceClient && this.persistenceClient != null) {
                this.persistenceClient.storeData(resourceName.getResourceName(), qos, data);
            }

            this.handleIncomingDataAnalysis(resourceName, data);
            this.handleUpstreamTransmission(resourceName, jsonData, data, qos);

            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
    {
        if (data != null) {
            _Logger.info("Handling system performance message: " + data.getName());

            if (data.hasError()) {
                _Logger.warning("Error flag set for SystemPerformanceData instance.");
            }

            String jsonData = DataUtil.getInstance().systemPerformanceDataToJson(data);

            int qos = ConfigConst.DEFAULT_QOS;

            //this.handleIncomingDataAnalysis(resourceName, data);
            this.handleUpstreamTransmission(resourceName, jsonData, data, qos);

            return true;
        } else {
            return false;
        }
    }


    private void handleIncomingDataAnalysis(ResourceNameEnum resource, ActuatorData data) {
        _Logger.info("Analyzing incoming actuator data: " + data.getName());

        if (data.isResponseFlagEnabled()) {

        } else {
            if (this.actuatorDataListener != null) {
                this.actuatorDataListener.onActuatorDataUpdate(data);
            }
        }
    }

    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SensorData data) {
    // Verificar si el tipo de sensor es de humedad
    if (data.getTypeID() == ConfigConst.HUMIDITY_SENSOR_TYPE) {
        handleHumiditySensorAnalysis(resourceName, data);
    }
}

    private void handleHumiditySensorAnalysis(ResourceNameEnum resourceName, SensorData data) {
        _Logger.fine("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());

        boolean isLow = data.getValue() < this.triggerHumidifierFloor;
        boolean isHigh = data.getValue() > this.triggerHumidifierCeiling;

        // Si la humedad está fuera de los umbrales establecidos
        if (isLow || isHigh) {
            _Logger.fine("Humidity data from CDA exceeds nominal range.");

            // Si no se ha registrado un dato de humedad anterior
            if (this.latestHumiditySensorData == null) {
                // Configura el estado inicial
                this.latestHumiditySensorData = data;
                this.latestHumiditySensorTimeStamp = getDateTimeFromData(data);

                _Logger.fine("Starting humidity nominal exception timer. Waiting for seconds: " + this.humidityMaxTimePastThreshold);
                return; // Salir hasta que llegue un nuevo dato
            } else {
                // Si ya hay datos anteriores, calcula la diferencia de tiempo
                OffsetDateTime curHumiditySensorTimeStamp = getDateTimeFromData(data);
                long diffSeconds = ChronoUnit.SECONDS.between(this.latestHumiditySensorTimeStamp, curHumiditySensorTimeStamp);

                _Logger.fine("Checking Humidity value exception time delta: " + diffSeconds);

                // Si ha pasado el tiempo máximo definido sin que se haya corregido el valor
                if (diffSeconds >= this.humidityMaxTimePastThreshold) {
                    // Crear un nuevo objeto ActuatorData para enviar al CDA
                    ActuatorData ad = new ActuatorData();
                    ad.setName(ConfigConst.HUMIDIFIER_ACTUATOR_NAME);
                    ad.setLocationID(data.getLocationID());
                    ad.setTypeID(ConfigConst.HUMIDIFIER_ACTUATOR_TYPE);
                    ad.setValue(this.nominalHumiditySetting);

                    // Si la humedad está demasiado baja, enciende el humidificador
                    if (isLow) {
                        ad.setCommand(ConfigConst.ON_COMMAND);
                    } else if (isHigh) {
                        ad.setCommand(ConfigConst.OFF_COMMAND);
                    }

                    // Enviar el comando al CDA
                    _Logger.info("Humidity exceptional value reached. Sending actuation event to CDA: " + ad);
                    this.lastKnownHumidifierCommand = ad.getCommand();
                    sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);

                    // Resetear los datos de humedad
                    this.latestHumidifierActuatorData = ad;
                    this.latestHumiditySensorData = null;
                    this.latestHumiditySensorTimeStamp = null;
                }
            }
        } else if (this.lastKnownHumidifierCommand == ConfigConst.ON_COMMAND) {
            // Si el humidificador está encendido, verificar si debe apagarse
            if (this.latestHumidifierActuatorData != null) {
                // Si la humedad ha llegado al valor nominal, apagar el humidificador
                if (this.latestHumidifierActuatorData.getValue() >= this.nominalHumiditySetting) {
                    this.latestHumidifierActuatorData.setCommand(ConfigConst.OFF_COMMAND);

                    _Logger.info("Humidity nominal value reached. Sending OFF actuation event to CDA: " + this.latestHumidifierActuatorData);

                    sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestHumidifierActuatorData);

                    // Resetear el estado
                    this.lastKnownHumidifierCommand = this.latestHumidifierActuatorData.getCommand();
                    this.latestHumidifierActuatorData = null;
                    this.latestHumiditySensorData = null;
                    this.latestHumiditySensorTimeStamp = null;
                } else {
                    _Logger.fine("Humidifier is still on. Not yet at nominal levels (OK).");
                }
            } else {
                _Logger.warning("ERROR: ActuatorData for humidifier is null (shouldn't be). Can't send command.");
            }
        }
    }

    private OffsetDateTime getDateTimeFromData(BaseIotData data) {
        OffsetDateTime odt = null;

        try {
            // Intentar analizar la marca de tiempo ISO 8601 desde los datos del IoT
            odt = OffsetDateTime.parse(data.getTimeStamp());
        } catch (Exception e) {
            // Si no se puede analizar la marca de tiempo, usar la hora local
            _Logger.warning("Failed to extract ISO 8601 timestamp from IoT data. Using local current time.");

            // Utilizar la hora local como último recurso
            odt = OffsetDateTime.now();
        }

        return odt;
    }

    private void sendActuatorCommandtoCda(ResourceNameEnum resource, ActuatorData data) {
        // Enviar el comando de Actuador al CDA
        if (this.actuatorDataListener != null) {
            this.actuatorDataListener.onActuatorDataUpdate(data);
        }

        // Enviar el comando de Actuador al CDA utilizando MQTT
        if (this.enableMqttClient && this.mqttClient != null) {
            // Convertir ActuatorData a JSON
            String jsonData = DataUtil.getInstance().actuatorDataToJson(data);

            // Intentar publicar el mensaje usando MQTT
            if (this.mqttClient.publishMessage(resource, jsonData, ConfigConst.DEFAULT_QOS)) {
                _Logger.info("Published ActuatorData command from GDA to CDA: " + data.getCommand());
            } else {
                _Logger.warning("Failed to publish ActuatorData command from GDA to CDA: " + data.getCommand());
            }
        }
    }

    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
    {
        _Logger.fine("Handling incoming system state data analysis...");
    }


    private void handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, SensorData data, int qos)
{
        // Lógica para enviar los datos al servicio en la nube
        _Logger.info("Send JSON data to cloud service: " + resourceName);

        // Implementación con MQTT
        if (this.enableMqttClient && this.mqttClient != null) {
            if (this.mqttClient.publishMessage(resourceName, jsonData, qos)) {
                _Logger.info("Published SensorData to cloud: " + jsonData);
            } else {
                _Logger.warning("Failed to publish SensorData to cloud: " + jsonData);
            }
        }
        if (this.cloudClient != null) {
            boolean sent = this.cloudClient.sendEdgeDataToCloud(resourceName, data);
            if (sent) {
                _Logger.fine("Sent SensorData to cloud.");
            } else {
                _Logger.warning("Failed to send SensorData to cloud.");
            }
        }
    }

    private void handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, SystemPerformanceData data, int qos)
    {
         // Lógica para enviar los datos al servicio en la nube
        _Logger.info("Send JSON data to cloud service: " + resourceName);

        // Implementación con MQTT
        if (this.enableMqttClient && this.mqttClient != null) {
            if (this.mqttClient.publishMessage(resourceName, jsonData, qos)) {
                _Logger.info("Published SensorData to cloud: " + jsonData);
            } else {
                _Logger.warning("Failed to publish SensorData to cloud: " + jsonData);
            }
        }
        if (this.cloudClient != null) {
            boolean sent = this.cloudClient.sendEdgeDataToCloud(resourceName, data);
            if (sent) {
                _Logger.fine("Sent SystemPerformanceData to cloud.");
            } else {
                _Logger.warning("Failed to send SystemPerformanceData to cloud.");
            }
        }
    }


	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
	    if (listener != null) {
            this.actuatorDataListener = listener;
            _Logger.info("ActuatorDataListener set: " + listener.getClass().getSimpleName());
        }
	}
	
    public void startManager()
    {
        _Logger.info("Starting DeviceDataManager...");

        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.startManager();
        }

        if (this.mqttClient != null) {
            if (this.mqttClient.connectClient()) {
                _Logger.info("Successfully connected MQTT client to broker.");

                // Suscripciones necesarias (por ahora)
                int qos = ConfigConst.DEFAULT_QOS;

                // Suscribirse a los temas relevantes
                //this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
                //this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
                //this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
                //this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
            } else {
                _Logger.severe("Failed to connect MQTT client to broker.");
                // Manejar el error de conexión
            }
    }

        if (this.cloudClient != null) {
            this.cloudClient.connectClient();
        }

        if (this.sysPerfMgr != null) {
		    this.sysPerfMgr.startManager();
	}
	    if (this.enableCoapServer && this.coapServer != null) {
            if (this.coapServer.startServer()) {
                _Logger.info("CoAP server started.");
            } else {
                _Logger.severe("Failed to start CoAP server. Check log file for details.");
            }
        }

    }
	
    public void stopManager()
    {
        _Logger.info("Stopping DeviceDataManager...");

        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.stopManager();
        }

        if (this.mqttClient != null) {
        // Desuscribirse de los temas si es necesario
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);

            // Desconectar el cliente MQTT
        if(this.mqttClient.disconnectClient()){
            _Logger.info("Successfully disconnected MQTT client from broker.");
		} else {
			_Logger.severe("Failed to disconnect MQTT client from broker.");
		}
            }

        if (this.cloudClient != null) {
            this.cloudClient.disconnectClient();
        }
        if (this.enableCoapServer && this.coapServer != null) {
            if (this.coapServer.stopServer()) {
                _Logger.info("CoAP server stopped.");
            } else {
                _Logger.severe("Failed to stop CoAP server. Check log file for details.");
            }
        }
    }

	// private methods
	
	/**
	 * Initializes the enabled connections. This will NOT start them, but only create the
	 * instances that will be used in the {@link #startManager() and #stopManager()) methods.
	 * 
	 */
	private void initConnections()
	{
	}
	
}
