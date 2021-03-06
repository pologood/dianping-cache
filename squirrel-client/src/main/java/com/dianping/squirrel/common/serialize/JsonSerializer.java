package com.dianping.squirrel.common.serialize;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonSerializer extends AbstractSerializer {

    private static final String TypeField = "@type";

    private ObjectMapper mapper;

    public JsonSerializer() {
        init();
    }

    private void init() {
        mapper = new ObjectMapper();
        // 设置输出时包含属性的风格
        mapper.setSerializationInclusion(Include.NON_NULL);
        // 序列化时，忽略空的bean(即沒有任何Field)
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 序列化时，忽略在JSON字符串中存在但Java对象实际没有的属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 序列化时，输出对象的类名
        mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, TypeField);
    }

    @Override
    public String doToString(Object object) throws Exception {
        return mapper.writeValueAsString(object);
    }

    @Override
    public byte[] doToBytes(Object object) throws Exception {
        return mapper.writeValueAsBytes(object);
    }

    @Override
    public Object doFromString(String data) throws Exception {
        return mapper.readValue(data, Object.class);
    }

    @Override
    public Object doFromBytes(byte[] data) throws Exception {
        return mapper.readValue(data, Object.class);
    }

}
