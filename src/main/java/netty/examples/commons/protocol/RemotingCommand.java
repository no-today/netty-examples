package netty.examples.commons.protocol;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author no-today
 * @date 2022/05/25 15:44
 */
@Getter
@Setter
public class RemotingCommand {

    private static final AtomicLong requestId = new AtomicLong(1);

    /**
     * 魔数
     */
    public static final int MAGIC_CODE = 0xADC001;

    /**
     * 唯一请求ID
     */
    private long reqId;

    /**
     * 协议版本
     */
    private short version;

    /**
     * 终端类型
     */
    private short terminal;

    /**
     * 标志符(bitmap)
     * <p>
     * 0: request
     * 1: response
     * 2: oneway
     */
    private int flag;

    /**
     * 请求码标识调用的命令
     * 响应码标识处理结果
     */
    private int code;

    /**
     * 备注字段, 通常只有响应异常时携带
     */
    private String remark;

    /**
     * 消息体
     */
    private byte[] body;

    /**
     * 扩展字段
     */
    private Map<String, String> extFields;

    public void encode(ByteBuf out) {
        byte[] encodeJson = GsonUtils.encode(this);

        out.writeInt(MAGIC_CODE);
        out.writeInt(encodeJson.length);
        out.writeBytes(encodeJson);
    }

    public static RemotingCommand decode(ByteBuf in) {
        if (MAGIC_CODE != in.readInt()) {
            throw new RuntimeException("decode error, magic number error");
        }

        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        return GsonUtils.decode(bytes, RemotingCommand.class);
    }

    public void markResponseType() {
        this.flag |= 1;
    }

    public void markOneway() {
        this.flag |= 2;
    }

    public boolean isRequest() {
        return (this.flag & 1) == 0;
    }

    public boolean isOneway() {
        return (this.flag & 2) == 0;
    }

    public RemotingCommandType getType() {
        if (isRequest()) return RemotingCommandType.REQUEST_COMMAND;
        return RemotingCommandType.RESPONSE_COMMAND;
    }

    public static RemotingCommand createRequestCommand(int code, byte[] body, Map<String, String> extFields) {
        RemotingCommand command = new RemotingCommand();
        command.setReqId(requestId.getAndIncrement());

        command.setCode(code);
        command.setBody(body);
        command.setExtFields(extFields);

        return command;
    }

    public static RemotingCommand createResponseCommand(long reqId, int code, String remark, byte[] body, HashMap<String, String> extFields) {
        RemotingCommand command = new RemotingCommand();
        command.markResponseType();
        command.setReqId(reqId);

        command.setCode(code);
        command.setRemark(remark);
        command.setBody(body);
        command.setExtFields(extFields);

        return command;
    }

    public static RemotingCommand createResponseCommand(long reqId, int code, String remark) {
        return createResponseCommand(reqId, code, remark, null, null);
    }

    @Override
    public String toString() {
        return GsonUtils.toJsonString(this);
    }
}
