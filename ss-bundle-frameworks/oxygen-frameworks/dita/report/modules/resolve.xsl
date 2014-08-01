<?xml version="1.0" encoding="UTF-8"?>
<!-- 
    Copyright 2001-2011 Syncro Soft SRL. All rights reserved.
    This is licensed under Oxygen XML Editor EULA (http://www.oxygenxml.com/eula.html).
    Redistribution and use outside Oxygen XML Editor is forbidden without express 
    written permission (contact e-mail address support@oxygenxml.com).
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:oxyd="http://www.oxygenxml.com/ns/dita"
    version="2.0">
    
    <!-- Resolve map and topic references, adding in topics content. -->

    <!-- default recurssive copy -->
    <xsl:template match="node() | @*" mode="resolve resolve-base">
        <xsl:copy>
            <xsl:apply-templates select="node() | @*" mode="#current"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- resolve topic refs -->
    <xsl:template match="*[contains(@class, ' map/topicref ') and @href and (@format='dita' or not(@format))]" mode="resolve">
        <xsl:variable name="topic" select="document(@href, .)"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="resolve"/>
            <oxyd:topicref xml:base="{document-uri($topic)}">
                <xsl:apply-templates select="$topic" mode="resolve"/>
            </oxyd:topicref>
            <!-- copy eventual content of the topic ref -->
            <xsl:apply-templates select="node()" mode="resolve"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- resolve maprefs -->
    <xsl:template match="*[contains(@class, ' map/topicref ') and @format='ditamap']" priority="100" mode="resolve">
        <xsl:variable name="map" select="document(@href, .)"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="resolve"/>
            <oxyd:mapref xml:base="{document-uri($map)}">
                <xsl:apply-templates select="$map" mode="resolve"/>
            </oxyd:mapref>
            <!-- copy eventual content of the map ref -->
            <xsl:apply-templates select="node()" mode="resolve"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- topicset reference -->
    <xsl:template match="*[contains(@class, ' mapgroup-d/topicsetref ')]" priority="150" mode="resolve">
        <xsl:variable name="map" select="document(substring-before(@href, '#'), .)"/>
        <xsl:variable name="id" select="substring-after(@href, '#')"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="resolve"/>
            <oxyd:mapref xml:base="{document-uri($map)}">
                <xsl:apply-templates select="$map//*[@id=$id]" mode="resolve"/>
            </oxyd:mapref>
            <!-- copy eventual content of the map ref -->
            <xsl:apply-templates select="node()" mode="resolve"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- disable topic expasion inside reltables -->
    <xsl:template match="*[contains(@class, ' map/reltable ')]" mode="resolve">
        <xsl:apply-templates select="." mode="resolve-base"/>
    </xsl:template>
    
    <!-- Do not try to open resourse-only topics -->
    <xsl:template match="*[contains(@class, ' map/topicref ') and @processing-role='resource-only']" priority="200" mode="resolve">
        <xsl:copy>
            <xsl:apply-templates select="node() | @*" mode="resolve"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- CONREFs -->
    <xsl:template match="*[@conref]" mode="resolve-base resolve">
        <xsl:variable name="topicURI" select="substring-before(@conref, '#')"/>
        <xsl:variable name="idPart" select="substring-after(@conref, '#')"/>
        <xsl:variable name="topicID" select="if (contains($idPart, '/')) then substring-before($idPart, '/') else $idPart"/>
        <xsl:variable name="elementID" select="substring-after($idPart, '/')"/>
        <xsl:variable name="topicFile" select="document($topicURI, .)"/>
        <xsl:variable name="targetTopic" select="$topicFile//*[contains(@class, ' topic/topic ') and @id=$topicID]"/>
        <oxyd:conref xml:base="{document-uri($topicFile)}" element="{name()}">
            <xsl:apply-templates select="if ($elementID = '') then $targetTopic else $targetTopic//*[@id=$elementID]" mode="#current"/>
        </oxyd:conref>
    </xsl:template>
    
    
</xsl:stylesheet>