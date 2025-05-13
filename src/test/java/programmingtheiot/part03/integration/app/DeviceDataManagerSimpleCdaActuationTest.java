package programmingtheiot.part03.integration.app;

import org.junit.Test;
import static org.junit.Assert.*;
import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.app.DeviceDataManager;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.MqttClientConnector;


public class DeviceDataManagerSimpleCdaActuationTest {

    @Test
    public void testSendActuationEventsToCda() {
        // Crear una instancia de DeviceDataManager
        DeviceDataManager devDataMgr = new DeviceDataManager();

        // Asegúrate de que PiotConfig.props esté configurado correctamente
        devDataMgr.startManager();

        ConfigUtil cfgUtil = ConfigUtil.getInstance();

        // Configurar valores de prueba
        float nominalVal = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
        float lowVal = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
        float highVal = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");
        int delay = cfgUtil.getInteger(ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");

        // Secuencia de prueba 1
        generateAndProcessHumiditySensorDataSequence(devDataMgr, nominalVal, lowVal, highVal, delay);

        // Detener el manager después de la prueba
        devDataMgr.stopManager();
    }

    private void generateAndProcessHumiditySensorDataSequence(DeviceDataManager ddm, float nominalVal, float lowVal, float highVal, int delay) {
        SensorData sd = new SensorData();
        sd.setName("My Test Humidity Sensor");
        sd.setLocationID("constraineddevice001");
        sd.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);

        // Prueba con valores nominales
        sd.setValue(nominalVal);
        ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitForSeconds(2);

        sd.setValue(nominalVal); // Enviar el mismo valor
        ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitForSeconds(2);

        // Prueba con valor bajo (que debería activar el humidificador)
        sd.setValue(lowVal - 2);
        ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitForSeconds(delay + 1);

        // Prueba con valor más alto, lo que debería desactivar el humidificador
        sd.setValue(lowVal - 1);
        ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitForSeconds(delay + 1);

        sd.setValue(lowVal + 1); // Valor fuera del rango
        ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitForSeconds(delay + 1);

        sd.setValue(nominalVal); // Regresar al valor nominal
        ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        waitForSeconds(delay + 1);
    }

    private void waitForSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000); // Espera por el número de segundos especificado
        } catch (InterruptedException e) {
            // Ignorar la interrupción
        }
    }
}
