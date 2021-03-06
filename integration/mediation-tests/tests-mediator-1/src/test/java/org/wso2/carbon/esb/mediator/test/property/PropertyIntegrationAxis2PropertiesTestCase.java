/*
 *Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.esb.mediator.test.property;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.wso2.carbon.automation.extensions.servers.jmsserver.client.JMSQueueMessageProducer;
import org.wso2.carbon.automation.extensions.servers.jmsserver.controller.config.JMSBrokerConfigurationProvider;
import org.wso2.esb.integration.common.extensions.carbonserver.CarbonServerExtension;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import static org.testng.Assert.assertTrue;

/**
 * This class tests the functionality of ConcurrentConsumers and MaxConcurrentConsumers properties
 * Axis2 configuration in axis2.xml
 * <parameter name="propertyIntegrationAxis2PropertiesTestCase" locked="false">
 * <parameter name="java.naming.factory.initial" locked="false">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>
 * <parameter name="java.naming.provider.url" locked="false">tcp://localhost:61616</parameter>
 * <parameter name="transport.jms.ConnectionFactoryJNDIName" locked="false">QueueConnectionFactory</parameter>
 * <parameter name="transport.jms.ConnectionFactoryType" locked="false">queue</parameter>
 * <parameter name="transport.jms.ConcurrentConsumers" locked="false">50</parameter>
 * <parameter name="transport.jms.MaxConcurrentConsumers" locked="false">200</parameter>
 * </parameter>
 */

public class PropertyIntegrationAxis2PropertiesTestCase extends ESBIntegrationTest {

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
    }

    @Test(groups = {"wso2.esb"}, description = "Send messages using  ConcurrentConsumers "
            + "and MaxConcurrentConsumers Axis2 level properties")
    public void maxConcurrentConsumersTest() throws Exception {
        CarbonServerExtension.restartServer();

        super.init();  // after restart the server instance initialization
        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + getHostname() + ":1099/jmxrmi");
        HashMap<String, String[]> environment = new HashMap<>();
        String[] credentials = new String[]{"admin", "admin"};
        environment.put(JMXConnector.CREDENTIALS, credentials);

        MBeanServerConnection mBeanServerConnection = JMXConnectorFactory.
                connect(url, environment).getMBeanServerConnection();

        int beforeThreadCount = (Integer) mBeanServerConnection
                .getAttribute(new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME), "ThreadCount");

        String queueName = "SimpleStockQuoteService";

        for (int x = 0; x < 200; x++) {
            JMSQueueMessageProducer sender = new JMSQueueMessageProducer(
                    JMSBrokerConfigurationProvider.getInstance().getBrokerConfiguration());

            try {
                sender.connect(queueName);
                for (int i = 0; i < 3; i++) {
                    sender.pushMessage("<?xml version='1.0' encoding='UTF-8'?>"
                            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas." + "xmlsoap.org/soap/envelope/\""
                            + " xmlns:ser=\"http://services.samples\" xmlns:xsd=\"" + "http://services.samples/xsd\">"
                            + "   <soapenv:Header/>" + "   <soapenv:Body>" + "      <ser:placeOrder>"
                            + "         <ser:order>" + "            <xsd:price>100</xsd:price>"
                            + "            <xsd:quantity>2000</xsd:quantity>"
                            + "            <xsd:symbol>JMSTransport</xsd:symbol>" + "         </ser:order>"
                            + "      </ser:placeOrder>" + "   </soapenv:Body>" + "</soapenv:Envelope>");
                }
            } finally {
                sender.disconnect();
            }
        }

        int afterThreadCount = (Integer) mBeanServerConnection
                .getAttribute(new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME), "ThreadCount");

        assertTrue((afterThreadCount - beforeThreadCount) <= 150, "Expected thread count range" + " not met");
    }
}
