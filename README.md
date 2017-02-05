# WeChatMsg
拦截微信聊天内容
ref: [page](http://www.toutiao.com/a6383226448857596161/?tt_from=weixin&utm_campaign=client_share&app=explore_article&utm_source=weixin&iid=7716094146&utm_medium=toutiao_ios&wxshare_count=1)

拦截抢红包功能：http://blog.csdn.net/jwzhangjie/article/details/47205299 
使用
我们实现某个功能比如点击等需要找到对应的对象然后模拟点击事件，所以首先就是怎么样找到对象，下面说三种方式：
（1）findAccessibilityNodeInfosByText通过文字来实现查找，返回的是List<AccessibilityNodeInfo>，所以需要通过for循环来具体判断需要的关键字的对象
（2）findAccessibilityNodeInfosByViewId通过控件的id来查询，返回的是List<AccessibilityNodeInfo>,虽然也是返回List但是一般只有一个，查找的准确性高，不过需要系统的版本API>=18
 (3)搭配findAccessibilityNodeInfosByText来查找，在微信中使用uiautomatorviewer查看布局，发现不同的手机相同的控件id是不一样的，比如我们需要查询获取红包的数量时，需要先查找'元'，然后获取其父控件，然后查找金额所在的位置，这个是不变的。
2、对于返回功能
一般我们领取红包后进入红包详情界面，这时我们要返回到聊天界面使用uiautomatorviewer查看返回箭头，查看其属性他的clickable=false这样的话我们就无法通过
accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);来实现点击事件来实现返回的功能，不过查看AccessibilityService源码里面有对应的全局事件，下面说两种实现返回功能的方法
（1）查找界面上对应的返回按钮然后通过AccessibilityNodeInfo的
performAction(AccessibilityNodeInfo.ACTION_CLICK)实现点击，不过在操作之前先判断一下isCheckable()如果是false则无法实现其功能

（2）使用AccessibilityService的performGlobalAction的方法，介绍如下：
[java] view plain copy print?
/** 
     * Performs a global action. Such an action can be performed 
     * at any moment regardless of the current application or user 
     * location in that application. For example going back, going 
     * home, opening recents, etc. 
     * 
     * @param action The action to perform. 
     * @return Whether the action was successfully performed. 
     * 
     * @see #GLOBAL_ACTION_BACK 
     * @see #GLOBAL_ACTION_HOME 
     * @see #GLOBAL_ACTION_NOTIFICATIONS 
     * @see #GLOBAL_ACTION_RECENTS 
     */  
    public final boolean performGlobalAction(int action) {  
        IAccessibilityServiceConnection connection =  
            AccessibilityInteractionClient.getInstance().getConnection(mConnectionId);  
        if (connection != null) {  
            try {  
                return connection.performGlobalAction(action);  
            } catch (RemoteException re) {  
                Log.w(LOG_TAG, "Error while calling performGlobalAction", re);  
            }  
        }  
        return false;  
    }  
所以要实现返回功能只需要调用performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);，当然如果想实现Home，通知，最近的应用换成对应的action就可以了
3、涉及微信界面的类
[java] view plain copy print?
/** 
    * 微信的包名 
    */  
   static final String WECHAT_PACKAGENAME = "com.tencent.mm";  
   /** 
    * 拆红包类 
    */  
   static final String WECHAT_RECEIVER_CALSS = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";  
   /** 
    * 红包详情类 
    */  
   static final String WECHAT_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";  
   /** 
    * 微信主界面或者是聊天界面 
    */  
   static final String WECHAT_LAUNCHER = "com.tencent.mm.ui.LauncherUI";  
这里需要注意的是WECHAT_LAUNCHER，微信主界面以及聊天界面应该采用的FragmentActivity+Fragment这样导致如果用户进入到微信主界面则会调用AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED，导致再次进入微信聊天界面不会再调用AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED，而会调用AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED，而AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED只要内容改变后都会调用，所以一般是使用AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED来作为监测事件的，所以解决这个问题的方式就是加入判断条件：
（1）触发AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED这个事件搜索列表界面是否有"领取红包"字样，如果没有则设置一个变量
（2）如果没有触发AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED而触发了AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED，则去判断之前设置的变量综合来判断
4、增加红包获取量避免重复金额
在聊天界面的红包虽然会有"领取红包"的字样，但是其实是已经拆过的，判断的标识就是是否有"拆红包"，如果有拆红包则计算对应详情中的金额。
5、如何循环查询所有的子控件
[java] view plain copy print?
/** 
     * @param info 当前节点 
     * @param matchFlag 需要匹配的文字 
     * @param type  操作的类型 
     */  
    public void recycle(AccessibilityNodeInfo info, String matchFlag, int type) {  
        if (info != null) {  
            if (info.getChildCount() == 0) {  
                CharSequence desrc = info.getContentDescription();  
                switch (type) {  
                    case ENVELOPE_RETURN://返回  
                        if (desrc != null && matchFlag.equals(info.getContentDescription().toString().trim())) {  
                            if (info.isCheckable()) {  
                                info.performAction(AccessibilityNodeInfo.ACTION_CLICK);  
                            } else {  
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);  
                            }  
                        }  
                        break;  
                }  
            } else {  
                int size = info.getChildCount();  
                for (int i = 0; i < size; i++) {  
                    AccessibilityNodeInfo childInfo = info.getChild(i);  
                    if (childInfo != null) {  
                        Log.e(TAG, "index: " + i + " info" + childInfo.getClassName() + " : " + childInfo.getContentDescription()+" : "+info.getText());  
                        recycle(childInfo, matchFlag, type);  
                    }  
                }  
            }  
        }  
    }  
网上关于抢红包的源码比较多，由于其他原因我们这里的不会公布，可以根据网上的源码进行修改，能够实现功能：
（1）截取通知栏中有[微信红包]字样的通知，然后跳到微信红包界面
（2）进入群聊界面会自动查询当前界面所有"领取红包"，然后循环点击查找增加红包的概率
（3）准确的保存领取的红包金额和日期
