package com.app.mvc.zookeeper;import lombok.extern.slf4j.Slf4j;import org.apache.zookeeper.CreateMode;import org.apache.zookeeper.KeeperException;import org.apache.zookeeper.WatchedEvent;import org.apache.zookeeper.Watcher;import org.apache.zookeeper.Watcher.Event.KeeperState;import org.apache.zookeeper.ZooDefs.Ids;import org.apache.zookeeper.ZooKeeper;import org.apache.zookeeper.data.Stat;import java.io.IOException;import java.util.concurrent.CountDownLatch;/** * Zookeeper Client * * Created by jimin on 16/3/15. */@Slf4jpublic class ZookeeperClient implements Watcher {    private static final int SESSION_TIMEOUT = 10000;    private static final String CONNECTION_STRING = "127.0.0.1:2181";    private static final String ZK_PATH = "/jimin";    private ZooKeeper zk = null;    private CountDownLatch connectedSemaphore = new CountDownLatch(1);    /**     * 创建ZK连接     */    public void createConnection(String connectString, int sessionTimeout) {        this.releaseConnection();        try {            zk = new ZooKeeper(connectString, sessionTimeout, this);            connectedSemaphore.await();        } catch (InterruptedException | IOException e) {            log.error("zookeeper release connection failed", e);        }    }    /**     * 关闭ZK连接     */    public void releaseConnection() {        if (this.zk != null) {            try {                this.zk.close();            } catch (InterruptedException e) {                log.error("zookeeper release connection failed", e);            }        }    }    /**     * 创建节点     */    public boolean createPath(String path, String data) {        try {            String result = this.zk.create(path, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);            log.info("zookeeper create path success, path:{}, content:{}", result, data);        } catch (KeeperException | InterruptedException e) {            log.error("zookeeper create path failed, path:{}, data:{}", path, data, e);        }        return true;    }    /**     * 读取指定节点数据内容     *     * @param path 节点path     * @return     */    public String readData(String path) {        try {            return new String(this.zk.getData(path, false, null));        } catch (KeeperException | InterruptedException e) {            log.error("zookeeper read data failed, path:{}", path, e);        }        return "";    }    /**     * 更新指定节点数据内容     *     * @param path 节点path     * @param data 数据内容     * @return     */    public boolean writeData(String path, String data) {        try {            Stat stat = this.zk.setData(path, data.getBytes(), -1);            log.info("zookeeper write data success，path:{}, stat:{}", path, stat);        } catch (KeeperException | InterruptedException e) {            log.error("zookeeper write data failed, path:{}", path, e);        }        return false;    }    /**     * 删除指定节点     *     * @param path 节点path     */    public void deleteNode(String path) {        try {            this.zk.delete(path, -1);            log.info("delete node success，path:{}", path);        } catch (KeeperException | InterruptedException e) {            log.error("zookeeper delete data failed, path:{}", path, e);        }    }    /**     * 收到来自Server的Watcher通知后的处理。     */    @Override    public void process(WatchedEvent event) {        log.info("get informed, {} \n", event.getState());        if (KeeperState.SyncConnected == event.getState()) {            connectedSemaphore.countDown();        }    }    public static void main(String[] args) {        ZookeeperClient client = new ZookeeperClient();        client.createConnection(CONNECTION_STRING, SESSION_TIMEOUT);        if (client.createPath(ZK_PATH, "我是节点初始内容")) {            log.info("数据内容: " + client.readData(ZK_PATH) + "\n");            client.writeData(ZK_PATH, "更新后的数据");            log.info("数据内容: " + client.readData(ZK_PATH) + "\n");            client.deleteNode(ZK_PATH);        }        client.releaseConnection();    }}