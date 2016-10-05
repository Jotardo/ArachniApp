package htmlparser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import database.ConnectDB;

/**
 * @author Juanca
 *
 * Clase creada para la organización de la ejecución del fichero de configuración.
 *
 * 'multMainEntity'		Array que almacena cada una de las URLs de cada entidad perteneciente a la entidad principal múltiple.
 * 'multAttribute'		Array que almacena los valores de un atributo múltiple.
 * 'simpleAttribute'	Variable que almacena el valor de un atributo simple.
 * 'mainEntity_array'	Array que almacena los parámetros y reglas de la entidad principal.
 * 'confFile_array' 	Array que almacena los parámetros del fichero de configuración.
 * 'nextPage_array'		Array que almacena los parámetros y reglas de la función de página siguiente.
 * 'attributes_array'	Array que almacena los parámetros y reglas de los atributos de las entidades.
 * 'url'				Variable que almacena la URL de la entidad principal introducida en el fichero de configuración.
 * 'db'					Instancia de ConnectDB para la gestión de funciones de tratamiento de la base de datos.
 * 'iD'					Instancia de InfoDownloader para el uso de las funciones de dicha clase.
 */

public class InfoOrganizator{
	
	ArrayList<String> multMainEntity_array = new ArrayList<String>();
	ArrayList<String> multAttribute_array = new ArrayList<String>();
	String simpleAttribute;	
	ArrayList<String> mainEntity_array = new ArrayList<String>();
	ArrayList<String> confFile_array = new ArrayList<String>();
	ArrayList<String> nextPage_array = new ArrayList<String>();
	ArrayList<ArrayList<String>> attributes_array = new ArrayList<ArrayList<String>>();
	String url;
	ConnectDB db;
	InfoDownloader iD = new InfoDownloader();
	
	/**
	 * Constructor de la clase InfoOrganizator.
	 * 
	 * @param url, Variable que almacena la URL de la entidad principal.
	 * @param confFile_array, Array que almacena los parámetros del fichero de configuración.
	 * @param mainEntity_array, Array que almacena los parámetros y reglas de la entidad principal.
	 * @param nextPage_array, Array que almacena los parámetros y reglas de la función de página siguiente.
	 * @param attributes_array, Array que almacena los parámetros y reglas de los atributos de las entidades.
	 * @param db, Instancia de la clase ConnectDB.
	 */
	
	public InfoOrganizator(String url, ArrayList<String> confFile_array, ArrayList<String> mainEntity_array, ArrayList<String> nextPage_array, ArrayList<ArrayList<String>> attributes_array, ConnectDB db){
		this.url = url;
		this.confFile_array = confFile_array;
		this.mainEntity_array = mainEntity_array;
		this.nextPage_array = nextPage_array;
		this.attributes_array = attributes_array;
		this.db = db;
	}
	
	/**
	 * Función que dispone el inicio de la ejecución del programa a través de los datos introducidos en el fichero xml.
	 * Dependiendo de si la entidad principal es simple o múltiple la ejecución del programa seguirá un curso u otro.
	 * 
	 */
	
	public void mainExecution(){
		String mainEntitySize = mainEntity_array.get(0);
		String urlType = mainEntity_array.get(1);
		String mainEntityXpath;
		String urlRoot = mainEntity_array.get(3);
			
		if(mainEntitySize.contains("simple")){
			// No debe ejecutarse ninguna función de botón siguiente si solo se evalua una entidad.			
			simpleEntityExecution();
		}
		else{			
			mainEntityXpath = mainEntity_array.get(2);
			
			if(nextPage_array.isEmpty()){
				System.out.println("Downloading main entities...");
				multMainEntity_array = iD.downloadArray(url, mainEntityXpath);
				
				if(urlType.contains("incomplete")){
					multMainEntity_array = iD.completeURLs(multMainEntity_array, urlRoot);
				}
				
				//iD.showArrayData(multMainEntity);			
				multEntityExecution();
			}
			else{
				nextPageExecution(mainEntityXpath, urlRoot);
			}
		}
	}
	
	/**
	 * Función que ejecuta la descarga de las URLs de las diferentes páginas de las que se quiere extraer información.
	 * La ejecución seguirá un curso u otro dependiendo de si se quiere navegar entre las páginas a través de la regla
	 * xPath perteneciente a un botón o bien a través de un patrón especificado en la URL de la entidad principal.
	 * 
	 * @param mainEntityXpath, Regla xPath de la entidad principal.
	 * @param urlRoot, Raíz de la URL de la entidad principal.
	 */
	
	public void nextPageExecution(String mainEntityXpath, String urlRoot){		
		String nextPageType = nextPage_array.get(0);
		String nextPageSize = nextPage_array.get(1);
		String nextPageXpath = nextPage_array.get(2);
		String nextPageNumPages = nextPage_array.get(3);
		String urlType = mainEntity_array.get(1);
		
		switch(nextPageType){
		case "button":			
			multMainEntity_array = iD.nextPagesButton(url, nextPageSize, nextPageXpath, nextPageNumPages, mainEntityXpath, urlType, urlRoot);				
	
			break;
		case "pattern":
			//Hacer de otro modo			
			break;
		default:
			break;
		}
		
		//showMultMainEntity_array();
		multEntityExecution();
	}
	
	/**
	 * Función que realiza la descarga y almacenamiento de los valores de los atributos localizados en una única entidad.
	 * 
	 */
	
	public void simpleEntityExecution(){
		for(int i = 0; i < attributes_array.size(); i++){
			String attributeSize = attributes_array.get(i).get(0);
			String attributeRule = attributes_array.get(i).get(2);
			
			if(attributeSize.contains("simple")){
				simpleAttribute = iD.downloadString(url, attributeRule);
				
				try{
					insertValuesDB(simpleAttribute, i, url);
				}catch(SQLException e){
					e.printStackTrace();
				}
			}
			else{					
				multAttribute_array = iD.downloadArray(url, attributeRule);
				
				for(int j = 0; j < multAttribute_array.size(); j++){
					try{
						insertValuesDB(multAttribute_array.get(j), i, url);
					}catch(SQLException e){
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Función que realiza la descarga y almacenamiento de los valores de los atributos localizados en varias entidades.
	 * 
	 */
	
	public void multEntityExecution(){
		for(int i = 0; i < multMainEntity_array.size(); i++){
			for(int j = 0; j < attributes_array.size(); j++){
				String attributeSize = attributes_array.get(j).get(0);
				String attributeRule = attributes_array.get(j).get(2);
				
				if(attributeSize.contains("simple")){
					simpleAttribute = iD.downloadString(multMainEntity_array.get(i), attributeRule);
					
					try{
						insertValuesDB(simpleAttribute, j, multMainEntity_array.get(i));
					}catch (SQLException e){
						e.printStackTrace();
					}
				}
				else{						
					multAttribute_array = iD.downloadArray(multMainEntity_array.get(i), attributeRule);
					
					for(int k = 0; k < multAttribute_array.size(); k++){
						try{
							insertValuesDB(multAttribute_array.get(k), j, multMainEntity_array.get(i));
						}catch(SQLException e){
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	/**
	 * Función que introduce en la base de datos los valores de los atributos descargados anteriormente.
	 * 
	 * @param value, Valor que contiene el atributo que se ha descargado.
	 * @param index, Índice del atributo del que se quiere almacenar su valor. Se necesita para distinguir entre un atributo y otro.
	 * @throws SQLException
	 */
	
	public void insertValuesDB(String value, int index, String urlEntity) throws SQLException{
		int numEntity = urlEntity.hashCode();
		int confFileId = 0, webPortalId = 0, categoryId = 0;
		ResultSet confFileRS = db.getConfFileId(confFile_array.get(0));
		ResultSet webPortalRS = db.getWebPortalId(confFile_array.get(1));
		ResultSet categoryRS = db.getCategoryId(confFile_array.get(2));
		String nameAttribute = attributes_array.get(index).get(1);
		
		while(confFileRS.next()){
			confFileId = confFileRS.getInt(1);
		}
		
		while(webPortalRS.next()){
			webPortalId = webPortalRS.getInt(1);
		}
		
		while(categoryRS.next()){
			categoryId = categoryRS.getInt(1);
		}	
		
		db.insertStringParams(nameAttribute, value, numEntity, webPortalId, confFileId, categoryId);
	}
	
	/**
	 * Función para visualizar las URLs de las entidades de las que se van a extraer información.
	 * 
	 */
	
	public void showMultMainEntity_array(){
		System.out.println("********** List of main entities **********\n");
		
		if(!(multMainEntity_array == null)){
			for(int i = 0; i < multMainEntity_array.size(); i++){
				System.out.println(i+": "+multMainEntity_array.get(i));
			}
		}
		else{
			System.out.println("This attribute hasn't values");
		}
		
		System.out.println();
	}
	
	/**
	 * Función para visualizar los valores del array del atributo actual que está almacenado en la variable multAttribute_array.
	 *
	 */
	
	public void showMultAttribute_array(){
		if(!(multAttribute_array == null)){
			for(int i = 0; i < multAttribute_array.size(); i++){
				System.out.println(i+": "+multAttribute_array.get(i));
			}
		}
		else{
			System.out.println("This attribute hasn't values");
		}
	}
	
	/**
	 * Función para visualizar el valor del atributo actual que está almacenado en la variable simpleAttribute.
	 *
	 */
	
	public void showSimpleAttribute(){
		if(!(simpleAttribute.isEmpty())){
			System.out.println(simpleAttribute);
		}
		else{
			System.out.println("This attribute hasn't values");
		}
	}
}