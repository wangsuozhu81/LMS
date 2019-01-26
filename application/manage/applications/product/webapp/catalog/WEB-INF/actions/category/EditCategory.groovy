import org.ofbiz.base.util.HttpRequestFileUpload
import org.ofbiz.base.util.UtilProperties
import org.ofbiz.base.util.string.FlexibleStringExpander

if (productCategory) {
    context.productCategoryType = productCategory.getRelatedOne("ProductCategoryType")
}
context.tenantId= delegator.getDelegatorTenantId()
primaryParentCategory = null
primParentCatIdParam = request.getParameter("primaryParentCategoryId")
if (productCategory) {
    primaryParentCategory = productCategory.getRelatedOne("PrimaryParentProductCategory")
} else if (primParentCatIdParam) {
    primaryParentCategory = delegator.findOne("ProductCategory", [productCategoryId : primParentCatIdParam], false)
}
context.primaryParentCategory = primaryParentCategory


// make the image file formats
imageFilenameFormat = UtilProperties.getPropertyValue("catalog", "image.filename.format")
imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.server.path"), context)

while(imageServerPath.endsWith("/")) imageServerPath = imageServerPath.substring(0,imageServerPath.length()-1)
imageUrlPrefix = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.url.prefix"), context)
context.imageFilenameFormat = imageFilenameFormat
context.imageServerPath = imageServerPath
context.imageUrlPrefix = imageUrlPrefix

filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat)
context.imageNameCategory = imageUrlPrefix + "/" + filenameExpander.expandString([location : "categories", type : "category", id : productCategoryId])
context.imageNameLinkOne  = imageUrlPrefix + "/" + filenameExpander.expandString([location : "categories", type : "linkOne", id : productCategoryId])
context.imageNameLinkTwo  = imageUrlPrefix + "/" + filenameExpander.expandString([location : "categories", type : "linkTwo", id : productCategoryId])


// UPLOADING STUFF

forLock = new Object()
contentType = null
fileType = request.getParameter("upload_file_type")
if (fileType) {
    context.fileType = fileType

    String fileLocation = filenameExpander.expandString([location : "categories", type : fileType, id : productCategoryId])
    String filePathPrefix = ""
    String filenameToUse = fileLocation
    if (fileLocation.lastIndexOf("/") != -1) {
        filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1) // adding 1 to include the trailing slash
        filenameToUse = fileLocation.substring(fileLocation.lastIndexOf("/") + 1)
    }

    int i1
    if (contentType && (i1 = contentType.indexOf("boundary=")) != -1) {
        contentType = contentType.substring(i1 + 9)
        contentType = "--" + contentType
    }

    defaultFileName = filenameToUse + "_temp"
    uploadObject = new HttpRequestFileUpload()
    uploadObject.setOverrideFilename(defaultFileName)
    uploadObject.setSavePath(imageServerPath + "/" + filePathPrefix)
    uploadObject.doUpload(request)

    clientFileName = uploadObject.getFilename()
    if (clientFileName) {
        context.clientFileName = clientFileName
    }

    if (clientFileName) {
        if (clientFileName.lastIndexOf(".") > 0 && clientFileName.lastIndexOf(".") < clientFileName.length()) {
            filenameToUse += clientFileName.substring(clientFileName.lastIndexOf("."))
        } else {
            filenameToUse += ".jpg"
        }

        context.clientFileName = clientFileName
        context.filenameToUse = filenameToUse

        characterEncoding = request.getCharacterEncoding()
        imageUrl = imageUrlPrefix + "/" + filePathPrefix + java.net.URLEncoder.encode(filenameToUse, characterEncoding)

        try {
            file = new File(imageServerPath + "/" + filePathPrefix, defaultFileName)
            file1 = new File(imageServerPath + "/" + filePathPrefix, filenameToUse)
            try {
                file1.delete()
            } catch (Exception e) {
                System.out.println("error deleting existing file (not neccessarily a problem)")
            }
            file.renameTo(file1)
        } catch (Exception e) {
            e.printStackTrace()
        }

        if (imageUrl) {
            context.imageUrl = imageUrl
            productCategory.set(fileType + "ImageUrl", imageUrl)
            productCategory.store()
        }
    }
}