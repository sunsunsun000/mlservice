package cn.ml_tech.mx.mlservice.Util;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.ml_tech.mx.mlservice.base.MlServerApplication;
import cn.ml_tech.mx.mlservice.base.SocketInfo;
import cn.ml_tech.mx.mlservice.base.SocketModule;

/**
 * 创建时间: 2017/12/26
 * 创建人: Administrator
 * 功能描述:
 */

public class WifiConnectUtil {
    private static WifiConnectUtil wifiConnectUtil;
    private MlServerApplication mlServerApplication;
    private String ipAddress = "";
    private ExecutorService threadPool;//线程池
    private ServerSocket serverSocket;
    private ToastUtil toastUtil;
    private Context context;
    private PrintWriter printWriter;
    private InputStream inputStream;
    private Gson gson;
    private Operate operate;

    public WifiConnectUtil(Context context) {
        this.context = context;
        mlServerApplication = (MlServerApplication) context.getApplicationContext();
        gson = mlServerApplication.getGson();
        toastUtil = ToastUtil.getInstance(context);
        ipAddress = "192.168.3.135";
        threadPool = Executors.newCachedThreadPool();
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(8888);
                    while (true) {
                        LogUtil.out(LogUtil.Debug, "等待连接");
                        Socket server = serverSocket.accept();
                        printWriter = new PrintWriter(server.getOutputStream(), true);//这个自动刷新要注意一下
                        LogUtil.out(LogUtil.Debug, "链接成功 客户端ip地址为:"+server.getInetAddress().getHostAddress());
                        printWriter.println(MlConCommonUtil.CONNECTSUCESS);
                        inputStream = server.getInputStream();

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.out(LogUtil.Debug, "io异常");
                }
            }
        });

    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public static WifiConnectUtil getWifiConnectUtil(Context context) {
        if (wifiConnectUtil == null)
            wifiConnectUtil = new WifiConnectUtil(context);
        return wifiConnectUtil;
    }

    /**
     * 开始检测移动端发出的指令
     *
     * @param operate
     */
    public void startObserver(Operate operate) {
        //其实就是子线程while循环+接口回调啦
        this.operate = operate;
        LogUtil.out(LogUtil.Debug, "startObserver");
        new Thread() {
            @Override
            public void run() {
                super.run();
                 while (true) {
                    try {
                        if (inputStream != null) {
                            if (inputStream.available() != 0) {
                                //检测流中是否有数据传入
                                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                                String res = bufferedReader.readLine();
                                LogUtil.out(LogUtil.Debug, res);
                                SocketModule socketModule = gson.fromJson(res, SocketModule.class);//解析json
                                handlerOperate(socketModule);//处理数据
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        LogUtil.out(LogUtil.Debug, "startObserver IOException");
                    }
                }
            }
        }.start();
    }


    /**
     * 处理操作类型
     *
     * @param socketModule
     */
    private void handlerOperate(SocketModule socketModule) {
        String operateType = socketModule.getOperateType();//获取操作类型
        SocketInfo operateResult = null;
        switch (operateType) {//switch判断  响应不同的操作类型
            case MlConCommonUtil.LOGIN:
                operateResult = operate.login(socketModule.getSocketInfo());//业务类中逻辑操作获取结果
                socketModule.getSocketInfo().setBaseModule(operateResult.getBaseModule());//将结果填充
        }
        String res = gson.toJson(socketModule);//整体转换为json字符串
        LogUtil.out(LogUtil.Debug,"获取数据完成，响应移动端进行中");
        printWriter.println(res);
        LogUtil.out(LogUtil.Debug,"数据发送完成");

    }

    /**
     * 接口回掉
     */
    public interface Operate {
        SocketInfo login(SocketInfo socketModule);
    }
}
