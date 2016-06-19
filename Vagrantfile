# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "bento/ubuntu-16.04"
  # Enable provisioning with a shell script. Additional provisioners such as
  # Puppet, Chef, Ansible, Salt, and Docker are also available. Please see the
  # documentation for more information about their specific syntax and use.
  config.vm.provision "shell", inline: <<-SHELL
    sudo apt-get update
    sudo apt-get install git postgresql-9.5 postgresql-client-common   -y
    wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
    sudo mkdir /home/vagrant/bin
    sudo mv lein /home/vagrant/bin/
    echo "export PATH=\"$PATH:$HOME/bin\"" >> /home/vagrant/.bashrc
    sudo chmod +x /home/vagrant/bin/lein
    sudo apt-get install default-jdk -y
    sudo -u postgres createuser huh -P huh_password
    sudo -u postgres createdb huh -O huh
  SHELL
end
