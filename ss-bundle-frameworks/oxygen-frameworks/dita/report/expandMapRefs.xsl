<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    xmlns:oxyd="http://www.oxygenxml.com/ns/dita">

    <xsl:import href="modules/resolve.xsl"/>
    <xsl:output indent="yes"/>

    <xsl:template match="/">
        <!-- Get the DITA map and all its content in a resolved document -->
        <oxyd:mapref xml:base="{document-uri(.)}">
            <xsl:apply-templates select="/" mode="resolve"/>
        </oxyd:mapref>
    </xsl:template>
</xsl:stylesheet>
