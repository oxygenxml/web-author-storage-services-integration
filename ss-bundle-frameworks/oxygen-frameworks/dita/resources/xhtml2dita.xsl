<?xml version="1.0" encoding="UTF-8"?>
<!-- 
  Copyright 2001-2012 Syncro Soft SRL. All rights reserved.
 -->
<xsl:stylesheet version="2.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:e="http://www.oxygenxml.com/xsl/conversion-elements"
                xmlns:f="http://www.oxygenxml.com/xsl/functions"
                exclude-result-prefixes="xsl e f">

  <xsl:template match="e:h1[ancestor::e:dl]
                     | e:h1[ancestor::e:section] 
                     | e:h2[ancestor::e:dl] 
                     | e:h2[ancestor::e:section] 
                     | e:h3[ancestor::e:dl] 
                     | e:h3[ancestor::e:section] 
                     | e:h4[ancestor::e:dl] 
                     | e:h4[ancestor::e:section] 
                     | e:h5[ancestor::e:dl]
                     | e:h5[ancestor::e:section]
                     | e:h6[ancestor::e:dl]
                     | e:h6[ancestor::e:section]">
      <b>
       <xsl:apply-templates select="@* | node()"/>
    </b>
  </xsl:template>

  <xsl:template match="e:p">
      <xsl:choose>
          <xsl:when test="(parent::e:td | parent::e:th) and count(parent::*[1]/*) = 1">
               <xsl:apply-templates select="@* | node()"/>
          </xsl:when>
          <xsl:when test="parent::e:ul | parent::e:ol">
              <!-- EXM-27834  Workaround for bug in OpenOffice/LibreOffice -->
              <li>
                  <p>
                      <xsl:call-template name="keepDirection"/>
                      <xsl:apply-templates select="@* | node()"/>
                  </p>
              </li>
          </xsl:when>
          <xsl:otherwise>
              <p>
                  <xsl:call-template name="keepDirection"/>
                  <xsl:apply-templates select="@* | node()"/>
              </p>
          </xsl:otherwise>
      </xsl:choose>
  </xsl:template>
    
  <xsl:template match="e:span[preceding-sibling::e:p and not(following-sibling::*)]">
     <p>
        <xsl:call-template name="keepDirection"/>
        <xsl:apply-templates select="@* | node()"/>
     </p>
  </xsl:template>
     
  <xsl:template match="e:pre">
    <xsl:choose>
      <xsl:when test="($context.path.last.name = 'codeblock' or $context.path.last.name = 'pre') and $context.path.last.uri = ''">
         <xsl:apply-templates select="@* | node()"/>
      </xsl:when>
      <xsl:otherwise>
        <pre>
          <xsl:call-template name="keepDirection"/>
          <xsl:apply-templates select="@* | node()"/>
        </pre>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="e:code">
    <xsl:choose>
      <xsl:when test="($context.path.last.name = 'codeblock' or $context.path.last.name = 'pre') and $context.path.last.uri = ''">
           <xsl:apply-templates select="@* | node()"/>
      </xsl:when>
      <xsl:otherwise>
        <codeblock>
          <xsl:call-template name="keepDirection"/>
          <xsl:apply-templates select="@* | node()"/>
        </codeblock>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
   
  
  <!-- Hyperlinks -->
  <xsl:template match="e:a[starts-with(@href, 'https://') or
                                        starts-with(@href,'http://') or starts-with(@href,'ftp://')]" 
                          priority="1.5">
       <xsl:variable name="xref">
            <xref>
              <xsl:attribute name="href">
                <xsl:value-of select="normalize-space(@href)"/>
              </xsl:attribute>
              <xsl:attribute name="format">html</xsl:attribute>
              <xsl:attribute name="scope">external</xsl:attribute>
              <xsl:call-template name="keepDirection"/>
              <xsl:apply-templates select="@* | * | text()"/>
           </xref>
       </xsl:variable>
       <xsl:call-template name="insertParaInSection">
           <xsl:with-param name="childOfPara" select="$xref"/>
       </xsl:call-template>
  </xsl:template>
  
  <xsl:template match="e:a[contains(@href,'#')]" priority="0.6">
      <xsl:variable name="xref">
            <xref>
              <xsl:attribute name="href">
                <xsl:call-template name="makeID">
                  <xsl:with-param name="string" select="normalize-space(@href)"/>
                </xsl:call-template>
              </xsl:attribute>
              <xsl:call-template name="keepDirection"/>
              <xsl:apply-templates select="@* | * | text()"/>
            </xref>
      </xsl:variable>
      <xsl:call-template name="insertParaInSection">
          <xsl:with-param name="childOfPara" select="$xref"/>
      </xsl:call-template>
  </xsl:template>
  
  <xsl:template match="e:a[@name != '']" priority="0.6">
    <ph>
      <xsl:attribute name="id">
          <xsl:call-template name="makeID">
            <xsl:with-param name="string" select="normalize-space(@name)"/>
          </xsl:call-template>
      </xsl:attribute>
      <xsl:call-template name="keepDirection"/>
      <xsl:apply-templates select="@* | * | text()"/>
    </ph>
  </xsl:template>
  
  <xsl:template match="e:a[@href != '']">
      <xsl:variable name="xref">
        <xref>
          <xsl:attribute name="href">
            <xsl:call-template name="makeID">
              <xsl:with-param name="string" select="normalize-space(@href)"/>
            </xsl:call-template>
          </xsl:attribute>
          <xsl:call-template name="keepDirection"/>
          <xsl:apply-templates select="@* | * | text()"/>
        </xref>
      </xsl:variable>
      <xsl:call-template name="insertParaInSection">
          <xsl:with-param name="childOfPara" select="$xref"/>
      </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="makeID">
    <xsl:param name="string" select="''"/>
    <xsl:call-template name="getFilename">
      <xsl:with-param name="path" select="translate($string,' \()','_/_')"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="string.subst">
   <xsl:param name="string" select="''"/>
   <xsl:param name="substitute" select="''"/>
   <xsl:param name="with" select="''"/>
   <xsl:choose>
    <xsl:when test="contains($string,$substitute)">
     <xsl:variable name="pre" select="substring-before($string,$substitute)"/>
     <xsl:variable name="post" select="substring-after($string,$substitute)"/>
     <xsl:call-template name="string.subst">
      <xsl:with-param name="string" select="concat($pre,$with,$post)"/>
      <xsl:with-param name="substitute" select="$substitute"/>
      <xsl:with-param name="with" select="$with"/>
     </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
     <xsl:value-of select="$string"/>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:template>
  
  <!-- Images -->
  <xsl:template match="e:img">
    <xsl:variable name="pastedImageURL" 
              xmlns:URL="java:java.net.URL"
              xmlns:URLUtil="java:ro.sync.util.URLUtil"
              xmlns:UUID="java:java.util.UUID">
      <xsl:choose>
        <xsl:when test="namespace-uri-for-prefix('o', .) = 'urn:schemas-microsoft-com:office:office'">
          <!-- Copy from MS Office. Copy the image from user temp folder to folder of XML document
            that is the paste target. -->
          <xsl:variable name="imageFilename">
            <xsl:variable name="fullPath" select="URL:getPath(URL:new(translate(@src, '\', '/')))"/>
            <xsl:variable name="srcFile">
              <xsl:choose>
                <xsl:when test="contains($fullPath, ':')">
                  <xsl:value-of select="substring($fullPath, 2)"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$fullPath"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="getFilename">
              <xsl:with-param name="path" select="string($srcFile)"/>
            </xsl:call-template>
          </xsl:variable>
          <xsl:variable name="stringImageFilename" select="string($imageFilename)"/>
          <xsl:variable name="uid" select="UUID:hashCode(UUID:randomUUID())"/>
          <xsl:variable name="uniqueTargetFilename" select="concat(substring-before($stringImageFilename, '.'), '_', $uid, '.', substring-after($stringImageFilename, '.'))"/>
          <xsl:variable name="sourceURL" select="URL:new(translate(@src, '\', '/'))"/>
          <xsl:variable name="correctedSourceFile">
            <xsl:choose>
              <xsl:when test="contains(URL:getPath($sourceURL), ':')">
                <xsl:value-of select="substring-after(URL:getPath($sourceURL), '/')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="URL:getPath($sourceURL)"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:variable name="sourceFile" select="URLUtil:uncorrect($correctedSourceFile)"/>
          <xsl:variable name="targetURL" select="URL:new(concat($folderOfPasteTargetXml, '/', $uniqueTargetFilename))"/>
          <xsl:value-of select="substring-after(string($targetURL),
                substring-before(string(URLUtil:copyURL($sourceURL, $targetURL)), $uniqueTargetFilename))"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@src"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <image href="{$pastedImageURL}">
      <xsl:if test="@height != ''">
        <xsl:attribute name="height">
          <xsl:value-of select="@height"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@width != ''">
        <xsl:attribute name="width">
          <xsl:value-of select="@width"/>
        </xsl:attribute>
      </xsl:if>
    </image>
  </xsl:template>
  
  <xsl:template name="getFilename">
   <xsl:param name="path"/>
   <xsl:choose>
    <xsl:when test="contains($path,'/')">
     <xsl:call-template name="getFilename">
      <xsl:with-param name="path" select="substring-after($path,'/')"/>
     </xsl:call-template>
    </xsl:when>
     <xsl:when test="contains($path,'\')">
       <xsl:call-template name="getFilename">
         <xsl:with-param name="path" select="substring-after($path,'\')"/>
       </xsl:call-template>
     </xsl:when>
     <xsl:otherwise>
       <xsl:value-of select="$path"/>
     </xsl:otherwise>
   </xsl:choose>
  </xsl:template>
  

    <xsl:template match="e:ul">
        <ul>
            <xsl:apply-templates/>
        </ul>
    </xsl:template>
    
    
    <xsl:template match="e:ol">
        <ol>
            <xsl:apply-templates/>
        </ol>
    </xsl:template>


  <xsl:template match="e:kbd">
    <userinput>
      <xsl:call-template name="keepDirection"/>
      <xsl:apply-templates select="@* | node()"/>
    </userinput>
  </xsl:template>
  
  <xsl:template match="e:samp">
    <systemoutput>
      <xsl:call-template name="keepDirection"/>
      <xsl:apply-templates select="@* | node()"/>
    </systemoutput>
  </xsl:template>
  
  <xsl:template match="e:blockquote">
    <lq>
        <xsl:call-template name="keepDirection"/>
        <xsl:apply-templates select="@* | node()"/>
    </lq>
  </xsl:template>
  
  <xsl:template match="e:q">
    <q>
        <xsl:call-template name="keepDirection"/>
        <xsl:apply-templates select="@* | node()"/>
    </q>
  </xsl:template>
  
  <xsl:template match="e:dl">
    <dl>
    	<xsl:apply-templates select="@*"/>
    	<xsl:variable name="dataBeforeTitle" select="e:dd[empty(preceding-sibling::e:dt)]"/>
    	<xsl:if test="not(empty($dataBeforeTitle))">
    		<dlentry>
    			<dt/>
    			<xsl:for-each select="$dataBeforeTitle">
    				<xsl:apply-templates select="."/>
    			</xsl:for-each>
    		</dlentry>
    	</xsl:if>
    	<xsl:for-each select="e:dt">
    		<dlentry>
    			<xsl:apply-templates select="."/>
    			<xsl:apply-templates select="following-sibling::e:dd[current() is preceding-sibling::e:dt[1]]"/>
    		</dlentry>
    	</xsl:for-each>
    </dl>
  </xsl:template>
  
  <xsl:template match="e:dt">
    <dt>
        <xsl:call-template name="keepDirection"/>
        <xsl:apply-templates select="@* | node()"/>
    </dt>
  </xsl:template>
  
  <xsl:template match="e:dd">
    <dd>
        <xsl:call-template name="keepDirection"/>
        <xsl:apply-templates select="@* | node()"/>
    </dd>
  </xsl:template>
    
  <xsl:template match="e:li">
      <li>
          <xsl:call-template name="keepDirection"/>
          <xsl:apply-templates/>
      </li>
  </xsl:template>
          
  <xsl:template match="@id"> 
    <xsl:attribute name="id">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="@dir">
    <xsl:attribute name="dir">
      <xsl:value-of select="lower-case(.)"/>
    </xsl:attribute>
  </xsl:template>
    
  <xsl:template match="@*">
    <!--<xsl:message>No template for attribute <xsl:value-of select="name()"/></xsl:message>-->
  </xsl:template>
  
  <!-- Inline formatting -->
  <xsl:template match="e:b | e:strong">
      <xsl:variable name="bold">
          <b><xsl:apply-templates select="@* | node()"/></b>
      </xsl:variable>
      <xsl:if test="string-length(normalize-space($bold)) > 0">
          <xsl:call-template name="insertParaInSection">
              <xsl:with-param name="childOfPara" select="$bold"/>
          </xsl:call-template>
      </xsl:if>
  </xsl:template>
    
  <xsl:template match="e:i | e:em">
      <xsl:variable name="italic">
          <i><xsl:apply-templates select="@* | node()"/></i>
      </xsl:variable>
      <xsl:if test="string-length(normalize-space($italic)) > 0">
          <xsl:call-template name="insertParaInSection">
              <xsl:with-param name="childOfPara" select="$italic"/>
          </xsl:call-template>
      </xsl:if>
  </xsl:template>
    
  <xsl:template match="e:u">
      <xsl:variable name="underline">
          <u><xsl:apply-templates select="@* | node()"/></u>
      </xsl:variable>
      <xsl:if test="string-length(normalize-space($underline)) > 0">
          <xsl:call-template name="insertParaInSection">
              <xsl:with-param name="childOfPara" select="$underline"/>
          </xsl:call-template>
      </xsl:if>
  </xsl:template>
          
  <!-- Ignored elements -->
  <xsl:template match="e:hr"/>
  <xsl:template match="e:meta"/>
  <xsl:template match="e:style"/>
  <xsl:template match="e:script"/>
  <xsl:template match="e:p[normalize-space() = '' and count(*) = 0]" priority="0.6"/>
  <xsl:template match="text()">
   <xsl:choose>
    <xsl:when test="normalize-space(.) = ''"><xsl:text> </xsl:text></xsl:when>
    <xsl:otherwise><xsl:value-of select="translate(., '&#xA0;', ' ')"/></xsl:otherwise>
   </xsl:choose>
  </xsl:template>
  
  
  <!-- Table conversion -->
    
  <xsl:template match="e:table">
      <table>
          <xsl:apply-templates select="@*"/>
          <xsl:if test="e:caption">
              <title>
                  <xsl:call-template name="keepDirection"/>
                  <xsl:apply-templates select="e:caption/node()"/>
              </title>
          </xsl:if>
          <tgroup>
              <xsl:variable name="columnCount">
                  <xsl:for-each select="e:tr | e:tbody/e:tr | e:thead/e:tr">
                      <xsl:sort select="count(e:td | e:th)" data-type="number" order="descending"/>
                      <xsl:if test="position()=1">
                          <xsl:value-of select="count(e:td | e:th)"/>
                      </xsl:if>
                  </xsl:for-each>
              </xsl:variable>
              <xsl:attribute name="cols">
                  <xsl:value-of select="$columnCount"/>
              </xsl:attribute>
              <xsl:if test="e:tr/e:td/@rowspan 
                  | e:tr/e:td/@colspan
                  | e:tbody/e:tr/e:td/@rowspan 
                  | e:tbody/e:tr/e:td/@colspan
                  | e:thead/e:tr/e:th/@rowspan 
                  | e:thead/e:tr/e:th/@colspan">
                  <xsl:call-template name="generateColspecs">
                      <xsl:with-param name="count" select="number($columnCount)"/>
                  </xsl:call-template>
              </xsl:if>
              <xsl:apply-templates select="e:thead"/>
              <tbody>
                  <xsl:apply-templates select="e:tr | e:tbody/e:tr | text() | e:b | e:strong | e:i | e:em | e:u, e:tfoot/e:tr"/>
              </tbody>
          </tgroup>
      </table>
  </xsl:template>
  
  <xsl:template match="e:thead">
    <thead>
       <xsl:apply-templates select="@* | node()"/>
    </thead>
  </xsl:template>
  
  <xsl:template match="e:tr">
    <row>
       <xsl:apply-templates select="@* | node()"/>
    </row>
  </xsl:template>
  
    
  <xsl:template match="e:th | e:td">
    <xsl:variable name="position" select="count(preceding-sibling::*) + 1"/>
    <entry>
        <xsl:if test="number(@colspan) and @colspan > 1">
          <xsl:attribute name="namest">
            <xsl:value-of select="concat('col', $position)"/>
          </xsl:attribute>
          <xsl:attribute name="nameend">
            <xsl:value-of select="concat('col', $position + number(@colspan) - 1)"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:if test="number(@rowspan) and @rowspan > 1">
          <xsl:attribute name="morerows">
            <xsl:value-of select="number(@rowspan) - 1"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:call-template name="keepDirection"/>
        <xsl:apply-templates select="@* | node()"/>
    </entry>
  </xsl:template>
  

  <xsl:template name="generateColspecs">
    <xsl:param name="count" select="0"/>
    <xsl:param name="number" select="1"/>
    <xsl:choose>
      <xsl:when test="$count &lt; $number"/>
      <xsl:otherwise>
        <colspec>
          <xsl:attribute name="colnum">
            <xsl:value-of select="$number"/>
          </xsl:attribute>
          <xsl:attribute name="colname">
            <xsl:value-of select="concat('col', $number)"/>
          </xsl:attribute>
        </colspec>
        <xsl:call-template name="generateColspecs">
          <xsl:with-param name="count" select="$count"/>
          <xsl:with-param name="number" select="$number + 1"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  

    <xsl:template match="e:section">
        <xsl:if test="e:title">
            <xsl:choose>
                <xsl:when test="$context.path.last.name = 'body'">
                    <p><b><xsl:apply-templates select="e:title"/></b></p>
                </xsl:when>
                <xsl:otherwise>
                    <b><xsl:apply-templates select="e:title"/></b>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:apply-templates 
            select="node()[local-name() != 'title' and local-name() != 'section']"/>
        <xsl:apply-templates select="e:section"/>
    </xsl:template>
    
    
    <xsl:template name="insertParaInSection">
        <xsl:param name="childOfPara"/>
        <xsl:choose>
            <xsl:when test="parent::e:section">
                <p><xsl:copy-of select="$childOfPara"/></p>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$childOfPara"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="keepDirection">
        <xsl:choose>
            <xsl:when test="@dir">
                <xsl:attribute name="dir">
                    <xsl:value-of select="lower-case(@dir)"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:when test="@DIR">
                <xsl:attribute name="dir">
                    <xsl:value-of select="lower-case(@DIR)"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:when test="count(e:span[@dir]|e:span[@DIR]) = 1">
                <xsl:attribute name="dir">
                    <xsl:value-of select="lower-case((e:span/@dir|e:span/@DIR)[1])"/>
                </xsl:attribute>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>