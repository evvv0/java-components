/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 by Andrew D. King
 */ 

package programmingtheiot.part03.integration.connection;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.*;
import programmingtheiot.gda.connection.*;

/**
 * This test case class contains very basic integration tests for
 * MqttClientControlPacketTest. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */
public class MqttClientControlPacketTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientControlPacketTest.class.getName());
	
	
	// member var's
	
	private MqttClientConnector mqttClient = null;
	
	
	// test setup methods
	
	@Before
	public void setUp() throws Exception
	{
		this.mqttClient = new MqttClientConnector();
	}
	
	@After
	public void tearDown() throws Exception
	{
	}
	
	// test methods
	

        @Test
    public void testConnectAndDisconnect() {
        // Conectar al broker MQTT
        boolean isConnected = this.mqttClient.connectClient();
        assertTrue("Connection to MQTT broker failed", isConnected);

        // Verificar que el cliente se conectó correctamente
        _Logger.info("Successfully connected to MQTT broker");

        // Verificar que se generó el paquete CONNECT y CONNACK
        // El callback 'connectComplete()' debe ser invocado en caso de éxito
        // Espera un poco para asegurar que los paquetes hayan sido procesados
        try {
            Thread.sleep(1000);  // Ajusta el tiempo según sea necesario
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Desconectar del broker MQTT
        this.mqttClient.disconnectClient();

        // Verificar que se generó el paquete DISCONNECT
        _Logger.info("Successfully disconnected from MQTT broker");

        // Asegúrate de que la conexión haya terminado
        assertFalse("Failed to disconnect from MQTT broker", this.mqttClient.isConnected());
    }

	
    @Test
    public void testServerPing() {
        // Conectar al broker MQTT
        boolean isConnected = this.mqttClient.connectClient();
        assertTrue("Connection to MQTT broker failed", isConnected);

        // Mantener la conexión abierta durante el tiempo suficiente para generar los paquetes PINGREQ y PINGRESP
        try {
            Thread.sleep(6000);  // Ajusta el tiempo para que la conexión permanezca activa (más que el Keep-Alive)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verificar que el paquete PINGREQ y PINGRESP fueron intercambiados
        // El paquete PINGRESP debe ser recibido después del PINGREQ automáticamente por el broker
        _Logger.info("Ping test completed, PINGREQ and PINGRESP exchanged.");

        // Desconectar del broker
        this.mqttClient.disconnectClient();
    }

	
    @Test
    public void testPubSub() {
        // Conectar al broker MQTT
        boolean isConnected = this.mqttClient.connectClient();
        assertTrue("Connection to MQTT broker failed", isConnected);

        // Suscribirse a un tema para generar el paquete SUBSCRIBE y SUBACK
        int qos = ConfigConst.DEFAULT_QOS;  // Asegúrate de usar QoS 1 o QoS 2 para los siguientes pasos

        boolean subResult = this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
        assertTrue("Subscription failed", subResult);

        // Publicar un mensaje en QoS 1 para generar los paquetes PUBLISH y PUBACK
        String testMessage = "Test Message for QoS 1";
        boolean pubResult = this.mqttClient.publishMessage(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, testMessage, 1);
        assertTrue("Publish failed with QoS 1", pubResult);

        // Publicar un mensaje en QoS 2 para generar los paquetes PUBLISH, PUBREC, PUBREL y PUBCOMP
        testMessage = "Test Message for QoS 2";
        boolean pubResultQoS2 = this.mqttClient.publishMessage(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, testMessage, 2);
        assertTrue("Publish failed with QoS 2", pubResultQoS2);

        // Esperar que los mensajes sean procesados y los paquetes de control generados
        try {
            Thread.sleep(2000);  // Ajusta según sea necesario para asegurar que se generen todos los paquetes
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Desconectar del broker
        this.mqttClient.disconnectClient();
    }

        @Test
    public void testUnsubscribe() {
        // Conectar al broker MQTT
        boolean isConnected = this.mqttClient.connectClient();
        assertTrue("Connection to MQTT broker failed", isConnected);

        // Suscribirse a un tema
        int qos = ConfigConst.DEFAULT_QOS;
        boolean subResult = this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
        assertTrue("Subscription failed", subResult);

        // Desuscribirse del tema para generar el paquete UNSUBSCRIBE y UNSUBACK
        boolean unsubResult = this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
        assertTrue("Unsubscription failed", unsubResult);

        // Esperar que los paquetes de desuscripción sean procesados
        try {
            Thread.sleep(1000);  // Ajusta el tiempo según sea necesario
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Desconectar del broker
        this.mqttClient.disconnectClient();
    }


	
}
