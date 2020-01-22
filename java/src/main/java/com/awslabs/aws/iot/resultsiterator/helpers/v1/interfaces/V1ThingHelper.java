package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.iot.model.ThingAttribute;
import com.awslabs.aws.iot.resultsiterator.exceptions.ThingAttachedToPrincipalsException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface V1ThingHelper {
    Stream<String> listThingNames();

    Stream<ThingAttribute> listThingAttributes();

    void delete(String name) throws ThingAttachedToPrincipalsException;

    List<String> listPrincipals(String thingName);

    Stream<String> listPrincipalThings(String principal);

    void detachPrincipal(String thingName, String principal);

    List<String> detachPrincipals(String name);

    void deletePrincipal(String principal);

    boolean principalAttachedToImmutableThing(String principal);

    boolean isThingImmutable(String thingName);

    boolean isThingArnImmutable(String thingArn);

    Optional<ThingAttribute> getThingIfItExists(String thingArn);
}
