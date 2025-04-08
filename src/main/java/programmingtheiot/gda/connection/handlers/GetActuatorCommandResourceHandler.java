package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;


public class GetActuatorCommandResourceHandler extends CoapResource
		implements IActuatorDataListener
{
	// Static
	private static final Logger _Logger =
		Logger.getLogger(GetActuatorCommandResourceHandler.class.getName());

	// params
	private ActuatorData actuatorData = null;

	// Constructor
	public GetActuatorCommandResourceHandler(String resourceName)
	{
		super(resourceName);

		super.setObservable(true);
	}

	// Constructor that takes a ResourceNameEnum
	public GetActuatorCommandResourceHandler(ResourceNameEnum resource)
	{
		this(resource.getResourceName());
	}

	/**
	 * Called when actuator data is updated. This method updates the local actuator data
	 * and notifies all connected clients about the change.
	 *
	 * @param data Updated actuator data
	 * @return true if the update was successful, false otherwise
	 */
	@Override
	public boolean onActuatorDataUpdate(ActuatorData data)
	{
		if (data != null && this.actuatorData != null) {
			this.actuatorData.updateData(data);

			// Notify all connected clients that the data has changed
			super.changed();

			// Log the update
			_Logger.fine("Actuator data updated for URI: " + super.getURI() + ": Data value = " + this.actuatorData.getValue());

			return true;
		}

		return false;
	}

	/**
	 * Handles GET requests. When a GET request is received, it responds with the current
	 * actuator data or an error message if the data is unavailable.
	 *
	 * @param context The CoapExchange containing the request context
	 */
	@Override
	public void handleGET(CoapExchange context)
	{
		_Logger.info("Received GET request for resource: " + getName());
	    context.accept();

		if (this.actuatorData != null) {

            String jsonData = DataUtil.getInstance().actuatorDataToJson(this.actuatorData);


            context.respond(ResponseCode.CONTENT, jsonData, MediaTypeRegistry.APPLICATION_JSON);


            context.respond(ResponseCode.CONTENT, jsonData);
            _Logger.fine("Actuator data sent: " + jsonData);

        } else {

            context.respond(ResponseCode.NOT_FOUND, "No actuator data available");

            _Logger.warning("No actuator data available for URI: " + getURI());
        }
    }


	@Override
	public void handlePUT(CoapExchange context) {

	}

	@Override
	public void handlePOST(CoapExchange context) {

	}

	@Override
	public void handleDELETE(CoapExchange context) {

	}
}