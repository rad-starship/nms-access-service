package com.rad.server.access.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Fetch;
import org.springframework.data.repository.cdi.Eager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.jws.Oneway;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
public class User
{
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	private long	id;
	private String	firstName;
	private String	lastName;
	private String	email;
	@Column(unique=true)
	private String	userName;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;
	private long roleID;
	@ElementCollection(fetch = FetchType.EAGER)
	private List<Long> tenantsID;


	public User()
	{
		
	}

	public long getRoleID() {
		return roleID;
	}

	public List<Long> getTenantID() {
		return tenantsID;
	}

	/**
	 * @param firstName
	 * @param lastName
	 * @param email
	 * @param userName
	 */
	public User(String firstName,String lastName, String email,String userName,String password,long roleID,Long[] tenants)
	{
		tenantsID=new ArrayList<>();
		this.firstName = firstName;
		this.lastName=lastName;
		this.email = email;
		this.userName=userName;
		this.roleID=roleID;
		this.password=password;
		tenantsID.addAll(Arrays.asList(tenants));
	}

	public User(String firstName,String lastName, String email,String userName,String password,long roleID,ArrayList<Long> tenants)
	{
		this.firstName = firstName;
		this.lastName=lastName;
		this.email = email;
		this.userName=userName;
		this.roleID=roleID;
		this.password=password;
		tenantsID=tenants;
	}

	public User(User user){
		this.firstName=user.firstName;
		this.lastName=user.lastName;
		this.email=user.email;
	}

	public void addTenant(long id){
		tenantsID.add(id);
	}

	public long getId()
	{
		return this.id;
	}

	public void setRoleID(long roleID) {
		this.roleID = roleID;
	}

	public void setTenantsID(List<Long> tenantsID) {
		this.tenantsID = tenantsID;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getFirstName()
	{
		return this.firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public String getEmail()
	{
		return this.email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password)
	{
		this.password=password;
	}

	public void encodePassword(String password)
	{
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		this.password=encoder.encode(password);
	}

	@Override
	public String toString()
	{
		return "User [id=" + this.id + ", name=" + this.firstName+" "+this.lastName + ", email=" + this.email + "]";
	}
}