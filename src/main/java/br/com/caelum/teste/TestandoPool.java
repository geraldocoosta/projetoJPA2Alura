package br.com.caelum.teste;

import java.beans.PropertyVetoException;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import br.com.caelum.JpaConfigurator;

public class TestandoPool {

	public static void main(String[] args) throws PropertyVetoException, SQLException {

		ComboPooledDataSource ds = (ComboPooledDataSource) new JpaConfigurator().getDataSource();

		for (int i = 0; i < 20; i++) {
			ds.getConnection();
			System.out.println("Conexoes existentes: " + ds.getNumConnections());
			System.out.println("Conexoes ocupadas: " + ds.getNumBusyConnections());
			System.out.println("Conexoes ociosas: " + ds.getNumIdleConnections());
			System.out.println();
		}
	}
}
