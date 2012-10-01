<#macro signature signature entryType entryAccession greyBoxYDimension signatureLinkYDimension>
<#--<#global locationId=0>-->
    <#assign maxAcLength=16><#--Maximum signature accession length-->
    <#assign maxOverallLength=20> <#-- Maximum allowed length of accession and name -->
    <#assign acLength=signature.ac?length> <#-- Actual length of the signature accession, e.g. G3DSA:2.40.20.10 -->
    <#assign nameLength=signature.name?length> <#-- Actual length of the signature name, e.g. Pept_Ser_Cys -->
    <#assign maxNameLength=maxOverallLength?number - acLength?number> <#-- Initialise the maximum allowed length of name -->
    <#if ((acLength?number) > (maxAcLength?number))>
    <#-- Accession was too long therefore should actually subtract maxAcLength instead of acLength -->
        <#assign maxNameLength=maxOverallLength?number - maxAcLength?number>
    </#if>
<svg x="123px" y="${greyBoxYDimension}px" width="1210" height="200" viewBox="0 0 1210 200">
${signature.getMatchLocationsViewSvg(protein.length,entryColours,entryType,entryAccession,scale)}
</svg>

<#--Signature link-->
<#--Link text may be abbreviated therefore need to display the full text in the link title-->
<svg id="signatureLink" x="1058px" y="${signatureLinkYDimension}px">
    <use xlink:href="#blackArrowComponent"/>
    <text x="15px" y="10.5px" style="font-family:Verdana,Helvetica,sans-serif;stroke:none;">
        <#if signature.ac != signature.name>
        <a xlink:href="${signature.dataSource.getLinkUrl(signature.ac)}"
           target="_top" title="${signature.ac} (${signature.name})">
            <#else>
            <a xlink:href="${signature.dataSource.getLinkUrl(signature.ac)}"
               target="_top" title="${signature.ac}">
        </#if>
        <tspan style="text-decoration:underline;fill:#525252;font-size: 11px">
        <#--Accession is too long, need to truncate it-->
            <#if ((acLength?number) > (maxAcLength?number))>
            ${signature.ac?substring(0,maxAcLength?number - 3)}...
                <#else>${signature.ac}</#if>
        </tspan>
    </a>
        <#if signature.ac != signature.name>
            <tspan style="fill:#525252;font-size: 9px">
            <#--Name is too long, need to truncate it-->
                <#if ((nameLength?number) > (maxNameLength?number))>
                    (${signature.name?substring(0, maxNameLength?number - 3)}...)
                    <#else>(${signature.name})</#if>
            </tspan>
        </#if>
    </text>
</svg>
</#macro>