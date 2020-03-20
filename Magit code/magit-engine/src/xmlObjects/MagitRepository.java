//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.07.30 at 03:05:27 PM IDT 
//


package xmlObjects;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}location"/>
 *         &lt;element ref="{}MagitBlobs"/>
 *         &lt;element ref="{}MagitFolders"/>
 *         &lt;element ref="{}MagitCommits"/>
 *         &lt;element ref="{}MagitBranches"/>
 *         &lt;element name="MagitRemoteReference" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;all>
 *                   &lt;element ref="{}location"/>
 *                   &lt;element ref="{}name"/>
 *                 &lt;/all>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "location",
    "magitBlobs",
    "magitFolders",
    "magitCommits",
    "magitBranches",
    "magitRemoteReference"
})
@XmlRootElement(name = "MagitRepository")
public class MagitRepository {

    public MagitRepository() {
    }

    @XmlElement(required = true)
    protected String location;
    @XmlElement(name = "MagitBlobs", required = true)
    protected MagitBlobs magitBlobs;
    @XmlElement(name = "MagitFolders", required = true)
    protected MagitFolders magitFolders;
    @XmlElement(name = "MagitCommits", required = true)
    protected MagitCommits magitCommits;
    @XmlElement(name = "MagitBranches", required = true)
    protected MagitBranches magitBranches;
    @XmlElement(name = "MagitRemoteReference")
    protected MagitRemoteReference magitRemoteReference;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of the location property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the value of the location property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLocation(String value) {
        this.location = value;
    }

    /**
     * Gets the value of the magitBlobs property.
     *
     * @return
     *     possible object is
     *     {@link MagitBlobs }
     *
     */
    public MagitBlobs getMagitBlobs() {
        return magitBlobs;
    }

    /**
     * Sets the value of the magitBlobs property.
     *
     * @param value
     *     allowed object is
     *     {@link MagitBlobs }
     *
     */
    public void setMagitBlobs(MagitBlobs value) {
        this.magitBlobs = value;
    }

    /**
     * Gets the value of the magitFolders property.
     *
     * @return
     *     possible object is
     *     {@link MagitFolders }
     *
     */
    public MagitFolders getMagitFolders() {
        return magitFolders;
    }

    /**
     * Sets the value of the magitFolders property.
     *
     * @param value
     *     allowed object is
     *     {@link MagitFolders }
     *
     */
    public void setMagitFolders(MagitFolders value) {
        this.magitFolders = value;
    }

    /**
     * Gets the value of the magitCommits property.
     *
     * @return
     *     possible object is
     *     {@link MagitCommits }
     *
     */
    public MagitCommits getMagitCommits() {
        return magitCommits;
    }

    /**
     * Sets the value of the magitCommits property.
     *
     * @param value
     *     allowed object is
     *     {@link MagitCommits }
     *
     */
    public void setMagitCommits(MagitCommits value) {
        this.magitCommits = value;
    }

    /**
     * Gets the value of the magitBranches property.
     *
     * @return
     *     possible object is
     *     {@link MagitBranches }
     *
     */
    public MagitBranches getMagitBranches() {
        return magitBranches;
    }

    /**
     * Sets the value of the magitBranches property.
     *
     * @param value
     *     allowed object is
     *     {@link MagitBranches }
     *
     */
    public void setMagitBranches(MagitBranches value) {
        this.magitBranches = value;
    }

    /**
     * Gets the value of the magitRemoteReference property.
     *
     * @return
     *     possible object is
     *     {@link MagitRemoteReference }
     *
     */
    public MagitRemoteReference getMagitRemoteReference() {
        return magitRemoteReference;
    }

    /**
     * Sets the value of the magitRemoteReference property.
     *
     * @param value
     *     allowed object is
     *     {@link MagitRemoteReference }
     *
     */
    public void setMagitRemoteReference(MagitRemoteReference value) {
        this.magitRemoteReference = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;all>
     *         &lt;element ref="{}location"/>
     *         &lt;element ref="{}name"/>
     *       &lt;/all>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {

    })
    public static class MagitRemoteReference {

        @XmlElement(required = true)
        protected String location;
        @XmlElement(required = true)
        protected String name;

        /**
         * Gets the value of the location property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getLocation() {
            return location;
        }

        /**
         * Sets the value of the location property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setLocation(String value) {
            this.location = value;
        }

        /**
         * Gets the value of the name property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the value of the name property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

    }

}
