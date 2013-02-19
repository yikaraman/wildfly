/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.management.security;


import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a management security realm's LDAP-based Authorization resource.
 *
 *  @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 */
public class LdapAuthorizationResourceDefinition extends LdapResourceDefinition {

    public static final SimpleAttributeDefinition USERNAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USERNAME_ATTRIBUTE, ModelType.STRING, false)
            .setXmlName("username-attribute")
            .setAlternatives(ModelDescriptionConstants.ADVANCED_FILTER)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setValidateNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition GROUPS_DN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.GROUPS_DN, ModelType.STRING, true)
            .setXmlName("attribute")
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false)).setDefaultValue(new ModelNode(UserLdapCallbackHandler.DEFAULT_USER_DN))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition PATTERN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PATTERN, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition RESULT_PATTERN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RESULT_PATTERN, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition GROUP = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.GROUP, ModelType.INT, true)
            .setValidator(new LongRangeValidator(1, 9, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES). build();

    public static final SimpleAttributeDefinition REVERSE_GROUP = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.REVERSE_GROUP, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {
        CONNECTION, BASE_DN, RECURSIVE, USER_DN,USERNAME, ADVANCED_FILTER, GROUPS_DN, PATTERN, GROUP, RESULT_PATTERN, REVERSE_GROUP
    };

    public LdapAuthorizationResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.AUTHORIZATION, ModelDescriptionConstants.LDAP),
                ControllerResolver.getResolver("core.management.security-realm.authorization.ldap"),
                new LdapAuthorizationAddHandler(), new SecurityRealmChildRemoveHandler(true),
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler handler = new LdapAuthenticationWriteHandler();
        handler.registerAttributes(resourceRegistration);
    }

    private static class LdapAuthenticationWriteHandler extends SecurityRealmChildWriteAttributeHandler {

        private LdapAuthenticationWriteHandler() {
            super(ATTRIBUTE_DEFINITIONS);
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode ignored) throws OperationFailedException {
                    final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                    final ModelNode model = resource.getModel();
                    validateAttributeCombination(model);
                    validateAttributePatternCombination(model);
                    context.stepCompleted();
                }
            }, OperationContext.Stage.MODEL);
            super.execute(context, operation);
        }

    }

    private static class LdapAuthorizationAddHandler extends SecurityRealmChildAddHandler {

        private LdapAuthorizationAddHandler() {
            super(true, ATTRIBUTE_DEFINITIONS);
        }

        @Override
        protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
            validateAttributeCombination(operation);
            validateAttributePatternCombination(operation);
            super.updateModel(context, operation);
        }
    }

    protected static void validateAttributePatternCombination(ModelNode operation) throws OperationFailedException {
        boolean resultPattern = operation.hasDefined(ModelDescriptionConstants.RESULT_PATTERN);
        boolean groupDefined = operation.hasDefined(ModelDescriptionConstants.GROUP);
        boolean patternDefined = operation.hasDefined(ModelDescriptionConstants.PATTERN);
        if (resultPattern && !patternDefined) {
            throw MESSAGES.canNotBeNull(ModelDescriptionConstants.PATTERN);
        } else if (groupDefined && !patternDefined) {
            throw MESSAGES.canNotBeNull(ModelDescriptionConstants.PATTERN);
        }
    }
}
