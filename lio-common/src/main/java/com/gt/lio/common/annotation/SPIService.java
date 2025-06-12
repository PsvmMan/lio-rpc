package com.gt.lio.common.annotation;

import com.gt.lio.common.spi.LioServiceLoader;

import java.lang.annotation.*;

/**
 * SPI机制, 为实现类指定名称、标识
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SPIService {

    /**
     * 名称
     *
     * @return 服务名称
     */
    String value();

    /**
     * 编码
     * 例如hessian序列化，网络传输的时候我们肯定希望传输一个字节的数据就好，而不是"hessian" 这样的字符串
     *
     * @return 服务类型
     */
    byte code() default LioServiceLoader.USELESS_CODE;

}
