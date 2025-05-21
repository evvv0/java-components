/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 by Andrew D. King
 */ 

package programmingtheiot.part04.integration.connection;

import static org.junit.Assert.*;

import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.DefaultDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.app.DeviceDataManager;
import programmingtheiot.gda.connection.*;

/**
 * This test case class contains very basic integration tests for
 * CloudClientConnector. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */
public class CloudClientConnectorTest {
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnectorTest.class.getName());
	
	
	// member var's
	
	private List<ICloudClient> cloudClientList = null;
	private ICloudClient cloudClient = null;
	private boolean actuatorEventReceived = false;
	
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		this.cloudClient = new CloudClientConnector();
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (this.cloudClient != null) {
			this.cloudClient.disconnectClient();
		}
	}
	
	// test methods
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.UbidotsMqttCloudClientConnector#connectClient()}.
	 */
//	@Test
	public void testCloudClientConnectAndDisconnect()
	{
		this.cloudClient.setDataMessageListener(new DefaultDataMessageListener());
		
		assertTrue(this.cloudClient.connectClient());
		
		try {
			// sleep for a minute or so...
			
			Thread.sleep(60000L);
		} catch (Exception e) {
			// ignore
		}
		
		assertTrue(this.cloudClient.disconnectClient());
		
		_Logger.info("Test complete.");
	}
	
	/**
	 * Test method
	 */
	@Test
	public void testIntegratedCloudClientConnectAndDisconnect()
	{
		DeviceDataManager ddm = new DeviceDataManager();
		ddm.startManager();
		
		try {
			// sleep for a minute or so...
			
			Thread.sleep(60000L);
		} catch (Exception e) {
			// ignore
		}
		
		ddm.stopManager();
		
		_Logger.info("Test complete.");
	}
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.UbidotsMqttCloudClientConnector#publishMessage(programmingtheiot.common.ResourceNameEnum, java.lang.String, int)}.
	 */
//	@Test
	public void testPublishAndSubscribe()
	{
		this.cloudClient.setDataMessageListener(new DefaultDataMessageListener());
		
		assertTrue(this.cloudClient.connectClient());
		
		try {
			// sleep for a couple of seconds or so...
			// 
			// TODO: if cloudClient delegates to MqttClientConnector,
			// which in turn delegates to MqttAsyncClient, the timing
			// of the sleep cycle may need to be manually adjusted to
			// allow the connection to complete
			
			Thread.sleep(2000L);
		} catch (Exception e) {
			// ignore
		}
		
		SensorData sensorData = new SensorData();
		sensorData.setName(ConfigConst.TEMP_SENSOR_NAME);
		sensorData.setValue(92.0f);
		
		SystemPerformanceData sysPerfData = new SystemPerformanceData();
		sysPerfData.setCpuUtilization(34.7f);
		sysPerfData.setMemoryUtilization(39.8f);
		
		assertTrue(this.cloudClient.subscribeToCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE));
		
		try {
			// sleep for a few seconds...
			// 
			// TODO: if cloudClient delegates to MqttClientConnector,
			// which in turn delegates to MqttAsyncClient, the timing
			// of the sleep cycle may need to be manually adjusted to
			// allow the connection to complete (even though the method
			// call may assume success if using an async connect)
			
			Thread.sleep(5000L);
		} catch (Exception e) {
			// ignore
		}
		
		assertTrue(this.cloudClient.sendEdgeDataToCloud(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));
		assertTrue(this.cloudClient.sendEdgeDataToCloud(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData));
		
		try {
			// sleep for half a minute or so...
			
			Thread.sleep(30000L);
		} catch (Exception e) {
			// ignore
		}
		
		assertTrue(this.cloudClient.unsubscribeFromCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE));

		try {
			// sleep for a minute or so...
			
			Thread.sleep(50000L);
		} catch (Exception e) {
			// ignore
		}

		assertTrue(this.cloudClient.disconnectClient());

		try {
			// sleep for a couple of seconds or so...
			// 
			// TODO: if cloudClient delegates to MqttClientConnector,
			// which in turn delegates to MqttAsyncClient, the timing
			// of the sleep cycle may need to be manually adjusted to
			// allow the disconnect to complete (even though the method
			// call may assume success if using an async disconnect)
			
			Thread.sleep(2000L);
		} catch (Exception e) {
			// ignore
		}
	}


	/**
	 * Test 1: Publish a SensorData to the cloud and verify it's accepted.
	 */
	@Test
	public void test1_publishSensorData() {
		assertTrue("Cloud client failed to connect", this.cloudClient.connectClient());

		SensorData sensorData = new SensorData();
		sensorData.setName(ConfigConst.TEMP_SENSOR_NAME);
		sensorData.setValue(25.5f);

		assertTrue("Failed to send sensor data",
			this.cloudClient.sendEdgeDataToCloud(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));

		_Logger.info("SensorData published successfully.");
		sleep(5000); // espera para asegurar publicación
	}

	/**
	 * Test 2: Trigger actuator event and verify reception.
	 */
	@Test
	public void test2_triggerAndReceiveActuation() {
		this.cloudClient.setDataMessageListener(new DefaultDataMessageListener() {

			public void handleActuatorCommandMessage(ResourceNameEnum resource, ActuatorData data) {
				_Logger.info("ActuatorData recibido: " + data.toString());
				actuatorEventReceived = true;
			}
		});

		assertTrue("Cloud client failed to connect", this.cloudClient.connectClient());

		// Subscribirse a eventos
		assertTrue(this.cloudClient.subscribeToCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE));

		// Simular cruce de umbral
		for (int i = 0; i < 3; i++) {
			SensorData sensorData = new SensorData();
			sensorData.setName(ConfigConst.TEMP_SENSOR_NAME);
			sensorData.setValue(100.0f); // valor alto que debe disparar evento

			assertTrue("Failed to send high value sensor data",
				this.cloudClient.sendEdgeDataToCloud(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));

			sleep(3000); // Esperar a que la nube procese y publique evento
		}

		// Esperar recepción del evento
		sleep(15000);

		assertTrue("Actuator event not received", actuatorEventReceived);

		this.cloudClient.unsubscribeFromCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE);
	}

	/**
	 * Test 3: Prueba de integración end-to-end con GDA y CDA
	 * Este test requiere ejecución del sistema real.
	 */
	@Test
	public void test3_endToEnd_GDA_CDA_Cloud() {
		DeviceDataManager ddm = new DeviceDataManager();
		ddm.startManager();

		_Logger.info("GDA iniciado. Esperando eventos de CDA durante 5 minutos...");

		sleep(5 * 60 * 1000); // 5 minutos

		ddm.stopManager();

		_Logger.info("Prueba end-to-end completa. Verificar logs para flujo de datos.");
	}

	// Helper
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception e) {
			// Ignorar interrupción
		}
	}



}
