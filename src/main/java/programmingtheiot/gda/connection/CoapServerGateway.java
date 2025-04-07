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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.gda.connection.handlers.GenericCoapResourceHandler;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class CoapServerGateway
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CoapServerGateway.class.getName());

	static {
	CoapConfig.register();
	UdpConfig.register();
}
	// params
	
	private CoapServer coapServer = null;
	
	private IDataMessageListener dataMsgListener = null;
	
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param dataMsgListener
	 */
	public CoapServerGateway(IDataMessageListener dataMsgListener)
	{
		super();
		
		/*
		 * Basic constructor implementation provided. Change as needed.
		 */
		
		this.dataMsgListener = dataMsgListener;
		
		initServer();
	}

		
	// public methods
	

    public void addResource(ResourceNameEnum resource) {
        if (coapServer != null) {
            Resource coapResource = createResourceChain(resource);
            coapServer.add(coapResource);
            _Logger.log(Level.INFO, "Recurso agregado: " + resource.name());
        } else {
            _Logger.log(Level.SEVERE, "CoapServer no está inicializado.");
        }
    }

    public boolean hasResource(String name) {
        return coapServer.getRoot().getChildren().stream()
                .anyMatch(r -> r.getName().equals(name));
    }
	
	public void setDataMessageListener(IDataMessageListener listener)
{
        if (listener != null) {
            this.dataMsgListener = listener;
    }
}

    public boolean startServer()
    {
        try {
            if (this.coapServer != null) {
                this.coapServer.start();

                // for message logging
                for (Endpoint ep : this.coapServer.getEndpoints()) {
                    ep.addInterceptor(new MessageTracer());
                }

                return true;
            } else {
                _Logger.warning("CoAP server START failed. Not yet initialized.");
            }
        } catch (Exception e) {
            _Logger.log(Level.SEVERE, "Failed to start CoAP server.", e);
        }

        return false;
    }

    public boolean stopServer()
    {
        try {
            if (this.coapServer != null) {
                this.coapServer.stop();

                return true;
            } else {
                _Logger.warning("CoAP server STOP failed. Not yet initialized.");
            }
        } catch (Exception e) {
            _Logger.log(Level.SEVERE, "Failed to stop CoAP server.", e);
        }

        return false;
    }

	
	// private methods
	
	private Resource createResourceChain(ResourceNameEnum resource) {
        CoapResource coapResource = new CoapResource(resource.name()) {
            @Override
            public void handleGET(CoapRequest request) {
                CoapResponse response = new CoapResponse("GET response for " + getName());
                respond(response);
            }

            @Override
            public void handlePUT(CoapRequest request) {
                CoapResponse response = new CoapResponse("PUT response for " + getName());
                respond(response);
            }

            @Override
            public void handlePOST(CoapRequest request) {
                CoapResponse response = new CoapResponse("POST response for " + getName());
                respond(response);
            }

            @Override
            public void handleDELETE(CoapRequest request) {
                CoapResponse response = new CoapResponse("DELETE response for " + getName());
                respond(response);
            }
        };

        return coapResource;
    }

    private void initServer(ResourceNameEnum... resources) {
        coapServer = new CoapServer();
        for (ResourceNameEnum resource : resources) {
            Resource coapResource = createResourceChain(resource);
            coapServer.add(coapResource);
        }
        coapServer.start();
    }
}
