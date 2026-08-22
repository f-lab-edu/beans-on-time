package com.bluetoya.beansontime.security.application;

import com.bluetoya.beansontime.security.model.ActorIdentity;

public interface CurrentActorProvider {

    ActorIdentity getCurrentActor();
}
