<?xml version="1.0" encoding="UTF-8"?>
<!--
    Copyright 2011 Jarno Elovirta
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    
    Modified 05 Jul 2011 by Syncrosoft to add a pattern for checking unique element IDs.
-->
<schema xmlns="http://purl.oclc.org/dsdl/schematron" 
    queryBinding="xslt2"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <title>Schematron schema for DITA 1.2</title>
    <p>Version 3.0.0 released 2010-10-17.</p>
    <ns uri="http://dita.oasis-open.org/architecture/2005/" prefix="ditaarch"/>
    
    <!--EXM-21448 Report duplicate IDs start pattern-->
    <xsl:key name="elementsByID" match="*[@id][not(contains(@class, ' topic/topic '))]"
        use="concat(@id, '#', ancestor::*[contains(@class, ' topic/topic ')][1]/@id)"/>
    
    <pattern id="checkIDs">
        <rule context="*[@id]">
            <let name="k" value="concat(@id, '#', ancestor::*[contains(@class, ' topic/topic ')][1]/@id)"/>
            <let name="countKey" value="count(key('elementsByID', $k))"/>
            <report test="$countKey > 1" see="http://docs.oasis-open.org/dita/v1.1/OS/archspec/id.html">
                The id attribute value "<value-of select="@id"/>" is not unique within the topic that contains it.
            </report>
        </rule>
    </pattern>
    <!--EXM-21448 Report duplicate IDs end pattern-->
    
    <pattern abstract="true" id="self_nested_element">
    <rule context="$element">
        <report test="descendant::$element" role="warning">The <name/> contains a <name/>. The
            results in processing are undefined.</report>
    </rule>
    </pattern>
    <pattern abstract="true" id="nested_element">
        <rule context="$element">
            <report test="descendant::$descendant">The <name/> contains <value-of
                    select="name(descendant::$descendant)"/>. Using <value-of
                    select="name(descendant::$descendant)"/> in this context is
                ill-adviced.</report>
        </rule>
    </pattern>
    <pattern abstract="true" id="future_use_element">
        <rule context="$context">
            <report test="$element">The <value-of select="name($element)"/> element is reserved for
                future use. <value-of select="$reason"/></report>
        </rule>
    </pattern>
    <pattern abstract="true" id="future_use_attribute">
        <rule context="$context">
            <report test="$attribute">The <value-of select="name($attribute)"/> attribute on <name/>
                is reserved for future use. <value-of select="$reason"/></report>
        </rule>
    </pattern>
    <pattern abstract="true" id="deprecated_element">
        <rule context="$context">
            <report test="$element">The <value-of select="name($element)"/> element is deprecated.
                    <value-of select="$reason"/></report>
        </rule>
    </pattern>
    <pattern abstract="true" id="deprecated_attribute">
        <rule context="$context">
            <report test="$attribute">The <value-of select="name($attribute)"/> attribute is
                deprecated. <value-of select="$reason"/></report>
        </rule>
    </pattern>
    <pattern abstract="true" id="deprecated_attribute_value">
        <rule context="$context">
            <report test="$attribute[. = $value]">The value "<value-of select="$value"/>" for
                    <value-of select="name($attribute)"/> attribute is deprecated. <value-of
                    select="$reason"/></report>
        </rule>
    </pattern>
    <pattern id="otherrole"
        see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/common/theroleattribute.html">
        <rule context="*[@role = 'other']">
            <assert test="@otherrole" role="error"><name/> with role 'other' should have otherrole
                attribute set.</assert>
        </rule>
    </pattern>
    <pattern id="othernote"
        see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/common/thetypeattribute.html">
        <rule context="*[contains(@class,' topic/note ')][@type = 'other']">
            <assert test="@othertype" role="error"><name/> with type 'other' should have othertype
                attribute set.</assert>
        </rule>
    </pattern>
    <pattern id="indextermref"
        see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/indextermref.html">
        <rule context="*">
            <report test="*[contains(@class, ' topic/indextermref ')]" role="error">The <value-of
                    select="name(*[contains(@class, ' topic/indextermref ')])"/> element is reserved
                for future use.</report>
        </rule>
    </pattern>
    <pattern id="collection-type_on_rel"
        see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/common/topicref-atts.html">
        <rule
            context="*[contains(@class, ' map/reltable ')] | *[contains(@class, ' map/relcolspec ')]">
            <report test="@collection-type" role="error">The collection-type attribute on <name/> is
                reserved for future use.</report>
        </rule>
    </pattern>
    <diagnostics>
        <diagnostic id="external_scope_attribute">Use the scope="external" attribute to indicate
            external links.</diagnostic>
        <diagnostic id="navtitle_element">Preferred way to specify navigation title is navtitle
            element.</diagnostic>
        <diagnostic id="state_element">The state element should be used instead with value attribute
            of "yes" or "no".</diagnostic>
        <diagnostic id="alt_element">The alt element should be used instead.</diagnostic>
        <diagnostic id="link_target">Elements with titles are candidate targets for elements level
            links.</diagnostic>
        <diagnostic id="title_links">Using <value-of
                select="name(descendant::*[contains(@class, ' topic/xref ')])"/> in this context is
            ill-adviced because titles in themselves are often used as links, e.g., in table of
            contents and cross-references.</diagnostic>
    </diagnostics>
</schema>
