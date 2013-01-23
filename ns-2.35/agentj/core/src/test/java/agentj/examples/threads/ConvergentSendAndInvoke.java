package agentj.examples.threads;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Jul 30, 2009
 * Time: 4:47:49 PM
 */
public class ConvergentSendAndInvoke {

 /**      public class ConvergentSender {

        private ServiceQuery query;
        private long[] timeouts;
        private boolean send;
        private List<ServiceInfo.Key> results = new Vector<ServiceInfo.Key>();
        private boolean useCache = false;
        private List<ServiceInfo> foundServersInfos = new Vector<ServiceInfo>();
        long minInvocationInterval = user.getConfiguration().getMinInvocationInterval();


        public ConvergentSender(ServiceQuery query, long[] multicastTimouts, boolean send) {
            this.query = query;
            this.timeouts = multicastTimouts;
            this.send = send;
            if (user.getConfiguration().isSupportServiceNotification() || user.getConfiguration().isSupportOppCaching()) {
                user.addServiceInterest(query);
                useCache = true;
                foundServersInfos = user.findCachedServices(query);
            }
        }

        public synchronized int converge(DatagramSocket socket, SrvRqst request, DatagramPacket packet, PacketContext context,
                                         SendHandler<PacketContext> handler) throws IOException {
            int timeoutIndex = 0;
            int currentConvergenceDropout = 0;
            int currLoop = 0;
            boolean discover = true;
            int totalRequired = user.getConfiguration().getMulticastMaxResults();
            int maximumConvergenceDropout = user.getConfiguration().getConvergenceDropout();

            for (timeoutIndex =0; timeoutIndex< timeouts.length; ++timeoutIndex) {
                log.debug("ConvergentDiscovery$ConvergentSender.converge TOP OF LOOP");
                if (useCache) {
                    foundServersInfos = user.findCachedServices(query);
                }
                int previousServersFound = foundServersInfos.size();

                if (results.size() < totalRequired && discover) {
                    log.debug(printNode(context) + "ConvergentDiscovery$ConvergentSender.converge no services discovered");
                    if (send) {
                        if (currLoop > 0) {
                            log.debug(printNode(context) + "ConvergentDiscovery$ConvergentSender.converge incrementing retry");
                            request.setRetryCount(request.getRetryCount() + 1);
                            byte[] bytes;
                            try {
                                bytes = request.serialize();
                            } catch (ServiceLocationException e) {
                                e.printStackTrace();
                                throw new IOException(e.getMessage());
                            }
                            packet = new DatagramPacket(bytes, bytes.length, packet.getSocketAddress());
                        }
                        log.debug(printNode(context) + "sending message from convergent send...");
                        handler.startSend(context);
                        socket.send(packet);
                        handler.endSend(context);
                    }
                    // first try and connect

                    int timeSpentConnecting = tryConnect(context, timeoutIndex, totalRequired);

                    if (results.size() >= totalRequired) {
                        log.debug(printNode(context) + "ConvergentDiscovery$ConvergentSender.converge got all results:" + results.size());
                        return results.size();
                    }

                    try {
                        log.debug(printNode(context) + "enter wait for " + timeouts[timeoutIndex]);
                        long now = System.currentTimeMillis();
                        long waittime = timeouts[timeoutIndex]-timeSpentConnecting;

                        long orgwaittime = waittime;
                        while (waittime > 0) {
                            // then wait for the remaining period in this interval

                            log.debug(printNode(context) + "in wait loop with waittime of " + waittime + " at time " + now);
                            wait(waittime);
                            long after = System.currentTimeMillis();
                            long diff = after - now;
                            log.debug(printNode(context) + "in wait with diff now being " + diff + " at time " + after);
                            if (diff <= 0) {
                                log.debug(printNode(context) + "finished waiting at time " + now + " at time " + after);
                                break;
                            }
                            waittime = orgwaittime - diff;
                        }
                        long after = System.currentTimeMillis();
                        log.debug(printNode(context) + "actual wait time:" + (after - now) + "loop started at: " + now + " at time " + after);
                    } catch (InterruptedException e) {
                    }
                }

                if (foundServersInfos.size() == previousServersFound) {
                    currentConvergenceDropout++;
                    // no new results for 'dropout' successive tries
                    if (currentConvergenceDropout == maximumConvergenceDropout) {
                        discover = false;
                    }
                } else {
                    currentConvergenceDropout = 0;
                }

                currLoop++;
            }
            return results.size();
        }

        private int tryConnect(PacketContext context, int timeoutIndex, int totalRequired) throws IOException  {
            int connectToServerIndex = 0;
            int timeSpent=0;

            if (useCache) {
                foundServersInfos = user.findCachedServices(query); // get it again
            }
            if (foundServersInfos.size() > 0) {     // Does the connect-to-server routine.
                log.debug(printNode(context) + "ConvergentDiscovery$ConvergentSender.converge got some services:" + foundServersInfos.size());
                long servers = Math.min(foundServersInfos.size(), timeouts[timeoutIndex] / minInvocationInterval);
                log.debug(printNode(context) + "ConvergentDiscovery$ConvergentSender.converge servers to connect to in wait interval:" + servers);

                for (int i = 0; i < servers; i++) {

                    int next = (i + connectToServerIndex);
                    next = next >= foundServersInfos.size() ? 0 : next;

                    log.debug(printNode(context) + "ConvergentDiscovery$ConvergentSender.converge next server:" + next);
                    ServiceInfo info = foundServersInfos.get(next);
                    String key = connect(info);
                    try {
                        long now = System.currentTimeMillis();
                        long waittime = minInvocationInterval;
                        long orgwaittime = waittime;
                        while (waittime > 0) {
                            log.debug(printNode(context) + "in wait loop with waittime of " + waittime + " at time " + now);
                            timeSpent+=waittime;
                            Thread.sleep(waittime);
                            long after = System.currentTimeMillis();
                            long diff = after - now;
                            if (diff <= 0) {
                                log.debug(printNode(context) + "finished waiting at time " + now);
                                break;
                            }
                            waittime = orgwaittime - diff;
                        }
                        log.debug(printNode(context) + "enter wait during invocation for " + waittime);

                        //log.debug(printNode(context) + "actual wait time during service invocation:" + (after - now));

                    } catch (InterruptedException e) {
                    }
                    if (isSuccess(key)) {
                        results.add(info.getKey());
                    }
                    if (results.size() >= totalRequired) {
                        log.debug(printNode(context) + "ConvergentDiscovery$ConvergentSender.converge got all results:" + results.size());
                        return timeSpent;
                    }
                }
                connectToServerIndex++;
            }
            if (useCache) {
                foundServersInfos = user.findCachedServices(query); // get it again
            }
            return timeSpent;
        }     **/
}
