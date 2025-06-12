package com.gt.lio.register;

import com.gt.lio.config.RegistryConfig;

public interface RegistryFactory {
    RegistryService getRegistry(RegistryConfig config);

}
