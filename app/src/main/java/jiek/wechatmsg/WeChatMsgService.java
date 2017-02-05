package jiek.wechatmsg;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * 服务的开启与关闭，是在设置-辅助功能-服务下的WeChatMsg,不是告诉后，右上角开启服务。来主动开关
 */
public class WeChatMsgService extends AccessibilityService {

    private static final String TAG = "WeChatMsgService";
    private String ChatUser;
    private String ChatTxt = "jiek";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            //聊天页有新消息时都会触发该滚屏事件
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                getWeChatLog(rootNode);
                break;
        }

    }

    /**
     * 遍历列表
     *
     * @param rootNode
     */
    private void getWeChatLog(AccessibilityNodeInfo rootNode) {
        if (rootNode != null) {
            //聊天列表的Node
            List<AccessibilityNodeInfo> listChatRecord = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/p");

            if (listChatRecord.size() == 0) {
                return;
            }
            //列表最后一条记录
            AccessibilityNodeInfo lastNode = listChatRecord.get(listChatRecord.size() - 1);
            List<AccessibilityNodeInfo> imageName = lastNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/id");
            //textview resource-id; 需要根据实际的ADM调试的布局提示值去修改，上同
            List<AccessibilityNodeInfo> textNode = lastNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/if");
            if (true || imageName.size() != 0) {
                Log.e(TAG, "getWeChatLog: " + textNode.size());
                if (textNode.size() == 0) {
                    //判断当前这条消息是不是和上一条一样，防止重复
                    if (!ChatTxt.equals("对方发的是图片或者表情")) {
                        //取聊天人
                        ChatUser = imageName.get(0).getContentDescription().toString().replace("头像", "");
                        //取聊天内容
                        ChatTxt = "对方发的是图片或者表情";

                        Log.e("消息为", ChatUser + "：" + "对方发的是图片或者表情");
                        Toast.makeText(this, ChatUser + "：" + ChatTxt, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "getWeChatLog: " + 88);
                    //防重复
                    if (!ChatTxt.equals(textNode.get(0).getText().toString())) {
                        //取聊天人
                        ChatUser = imageName.get(0).getContentDescription().toString().replace("头像", "");
                        //取聊天内容
                        ChatTxt = textNode.get(0).getText().toString();

                        Log.e("消息为", ChatUser + "：" + ChatTxt);
                        Toast.makeText(this, ChatUser + "：" + ChatTxt, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
        super.onServiceConnected();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "服务已关闭", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }
}
