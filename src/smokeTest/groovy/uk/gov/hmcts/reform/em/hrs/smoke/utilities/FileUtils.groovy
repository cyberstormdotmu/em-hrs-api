package uk.gov.hmcts.reform.em.hrs.smoke.utilities

class FileUtils {

    File getResourceFile(String fileName){
//        new File(getClass().getClassLoader().getResource(fileName).path.replace("%20", " "))
        new File(URLDecoder.decode(getClass().getClassLoader().getResource(fileName).path, "UTF-8"))
    }


}
