package netty.examples.commons.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author no-today
 * @date 2022/05/25 15:44
 */
public class RemotingCommand {

    /**
     * 魔数
     */
    public static final int MAGIC_CODE = 0xADC001;

    /**
     * 协议版本
     */
    private short version;

    /**
     * 终端类型
     */
    private short terminal;

    /**
     * 消息类型
     */
    private int type;

    /**
     * 唯一序列号
     */
    private long sequence;

    /**
     * 消息长度
     */
    private int length;

    /**
     * 消息体
     */
    private byte[] body;

    public RemotingCommand() {
    }

    public RemotingCommand(short version, short terminal, int type, long sequence) {
        this(version, terminal, type, sequence, null);
    }

    public RemotingCommand(short version, short terminal, int type, long sequence, byte[] body) {
        this.version = version;
        this.terminal = terminal;
        this.type = type;
        this.sequence = sequence;
        this.body = body;
        this.length = body != null ? body.length : 0;
    }

    public void encode(ByteBuf out) {
        out.writeInt(RemotingCommand.MAGIC_CODE);
        out.writeShort(version);
        out.writeShort(terminal);
        out.writeInt(type);
        out.writeLong(sequence);
        out.writeInt(length);
        out.writeBytes(body);
    }

    public static RemotingCommand decode(ByteBuf in) {
        if (MAGIC_CODE != in.readInt()) {
            throw new RuntimeException("decode error, magic number error");
        }

        RemotingCommand command = new RemotingCommand(
                in.readShort(),
                in.readShort(),
                in.readInt(),
                in.readLong());

        int length = in.readInt();
        byte[] body = new byte[length];
        in.readBytes(body);

        command.length = length;
        command.body = body;
        return command;
    }

    public short getVersion() {
        return version;
    }

    public RemotingCommand setVersion(short version) {
        this.version = version;
        return this;
    }

    public short getTerminal() {
        return terminal;
    }

    public RemotingCommand setTerminal(short terminal) {
        this.terminal = terminal;
        return this;
    }

    public int getType() {
        return type;
    }

    public RemotingCommand setType(int type) {
        this.type = type;
        return this;
    }

    public long getSequence() {
        return sequence;
    }

    public RemotingCommand setSequence(long sequence) {
        this.sequence = sequence;
        return this;
    }

    public int getLength() {
        return length;
    }

    public RemotingCommand setLength(int length) {
        this.length = length;
        return this;
    }

    public byte[] getBody() {
        return body;
    }

    public RemotingCommand setBody(byte[] body) {
        this.body = body;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RemotingCommand{");
        sb.append("version=").append(version);
        sb.append(", terminal=").append(terminal);
        sb.append(", type=").append(type);
        sb.append(", sequence=").append(sequence);
        sb.append(", length=").append(length);
        sb.append(", body=").append(new String(body, StandardCharsets.UTF_8));
        sb.append('}');
        return sb.toString();
    }
}
