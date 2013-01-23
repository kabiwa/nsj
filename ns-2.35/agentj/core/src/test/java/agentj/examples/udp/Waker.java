/*
 * Copyright 2004 - 2007 University of Cardiff.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package agentj.examples.udp;

import java.util.Random;

/**
 * Class Description Here...
 *
 * @author Andrew Harrison
 * @version $Revision: 1.1 $
 * @created Oct 10, 2008: 11:15:51 AM
 * @date $Date: 2008-10-22 19:47:50 $ modified by $Author: harrison $
 * @todo Put your notes here...
 */
public class Waker extends Thread {


    private WaitTest sender;
    private Random r = new Random();

    public Waker(WaitTest sender) {
        this.sender = sender;

    }

    public synchronized void run() {
        try {

            sleep(1000);
            sender.wakeUp(true);
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }


}